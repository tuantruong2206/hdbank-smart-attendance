package com.hdbank.attendance.application.service;

import com.hdbank.attendance.application.dto.CheckInCommand;
import com.hdbank.attendance.application.dto.CheckInResult;
import com.hdbank.attendance.application.port.in.CheckInUseCase;
import com.hdbank.attendance.application.port.in.CheckOutUseCase;
import com.hdbank.attendance.application.port.in.QrCodeUseCase;
import com.hdbank.attendance.application.port.out.AttendanceRepository;
import com.hdbank.attendance.application.port.out.EventPublisher;
import com.hdbank.attendance.application.port.out.LateGraceQuotaRepository;
import com.hdbank.attendance.application.port.out.LocationRepository;
import com.hdbank.attendance.application.port.out.ShiftRepository;
import com.hdbank.attendance.domain.exception.InvalidLocationException;
import com.hdbank.attendance.domain.model.AttendanceRecord;
import com.hdbank.attendance.domain.model.LateGraceQuota;
import com.hdbank.attendance.domain.model.Location;
import com.hdbank.attendance.domain.model.Shift;
import com.hdbank.attendance.domain.service.ConfigResolver;
import com.hdbank.attendance.domain.service.DuplicateCheckInGuard;
import com.hdbank.attendance.domain.service.FraudScorer;
import com.hdbank.attendance.domain.service.LocationVerifier;
import com.hdbank.attendance.domain.service.ShiftRuleEngine;
import com.hdbank.attendance.domain.valueobject.FraudScore;
import com.hdbank.attendance.domain.valueobject.ShiftEvaluation;
import com.hdbank.common.event.CheckInEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Core check-in/check-out service implementing 5-scenario validation:
 * 1. WiFi BSSID match (primary)
 * 2. WiFi miss + GPS inside geofence (secondary)
 * 3. GPS only (no WiFi)
 * 4. QR code scan
 * 5. Manual entry (admin override)
 *
 * Also integrates shift evaluation and late grace quota management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInService implements CheckInUseCase, CheckOutUseCase {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int DEFAULT_LATE_GRACE_MAX = 4;

    private final AttendanceRepository attendanceRepository;
    private final LocationRepository locationRepository;
    private final LocationVerifier locationVerifier;
    private final DuplicateCheckInGuard duplicateGuard;
    private final FraudScorer fraudScorer;
    private final EventPublisher eventPublisher;
    private final ShiftRuleEngine shiftRuleEngine;
    private final ShiftRepository shiftRepository;
    private final LateGraceQuotaRepository lateGraceQuotaRepository;
    private final QrCodeUseCase qrCodeUseCase;
    private final ConfigResolver configResolver;

    @Override
    @Transactional
    public CheckInResult checkIn(CheckInCommand command) {
        return processCheckInOut(command, AttendanceRecord.CheckType.CHECK_IN);
    }

    @Override
    @Transactional
    public CheckInResult checkOut(CheckInCommand command) {
        return processCheckInOut(command, AttendanceRecord.CheckType.CHECK_OUT);
    }

    private CheckInResult processCheckInOut(CheckInCommand command, AttendanceRecord.CheckType checkType) {
        // 1. Determine verification scenario (5 scenarios)
        VerificationOutcome verification = resolveVerification(command);

        // 2. Check duplicates (only for CHECK_IN)
        Instant checkTime = command.isOffline() ? command.offlineTimestamp() : Instant.now();
        if (checkType == AttendanceRecord.CheckType.CHECK_IN) {
            duplicateGuard.check(command.employeeId(), command.employeeType(), null, checkTime);
        }

        // 3. Score fraud (skip for MANUAL entries)
        Location location = verification.locationId != null
                ? locationRepository.findById(verification.locationId).orElse(null)
                : null;
        FraudScore fraudScore;
        if (verification.method == AttendanceRecord.VerificationMethod.MANUAL) {
            fraudScore = new FraudScore(0, java.util.List.of());
        } else {
            fraudScore = fraudScorer.score(command.deviceInfo(), command.gpsCoordinate(), location);
        }

        // 4. Resolve shift for this employee at check time
        Shift shift = shiftRepository.findCurrentShiftForEmployee(command.employeeId(), checkTime)
                .orElse(null);

        // 5. Create attendance record
        AttendanceRecord record = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .employeeId(command.employeeId())
                .employeeCode(command.employeeCode())
                .checkType(checkType)
                .checkTime(checkTime)
                .locationId(verification.locationId)
                .wifiBssid(verification.bssid)
                .wifiSsid(verification.ssid)
                .gpsLatitude(command.gpsCoordinate() != null ? command.gpsCoordinate().latitude() : null)
                .gpsLongitude(command.gpsCoordinate() != null ? command.gpsCoordinate().longitude() : null)
                .gpsAccuracy(command.gpsCoordinate() != null ? command.gpsCoordinate().accuracy() : null)
                .deviceId(command.deviceId())
                .deviceInfo(command.deviceInfo())
                .verificationMethod(verification.method)
                .status(AttendanceRecord.Status.VALID)
                .isOffline(command.isOffline())
                .offlineUuid(command.offlineUuid())
                .shiftId(shift != null ? shift.getId() : null)
                .notes(command.isManualEntry() ? command.manualReason() : null)
                .build();

        // 6. Apply fraud score
        if (fraudScore.isSuspicious()) {
            record.markSuspicious(fraudScore.score(), fraudScore.flags());
        }

        // 7. Evaluate shift rules and handle late grace quota
        String lateStatus = null;
        Integer lateGraceRemaining = null;

        if (shift != null && checkType == AttendanceRecord.CheckType.CHECK_IN) {
            ShiftEvaluation evaluation = shiftRuleEngine.evaluate(record, shift);

            if (evaluation.isLate()) {
                LateGraceResult graceResult = handleLateGraceQuota(
                        command.employeeId(), checkTime, evaluation);
                lateStatus = graceResult.lateStatus;
                lateGraceRemaining = graceResult.remaining;

                // Append late info to notes
                String lateNote = String.format("Trễ %d phút - %s",
                        evaluation.lateMinutes(), lateStatus);
                record.setNotes(record.getNotes() != null
                        ? record.getNotes() + "; " + lateNote : lateNote);
            }
        }

        // 8. Save record
        record = attendanceRepository.save(record);

        // 9. Publish event
        eventPublisher.publishCheckIn(CheckInEvent.builder()
                .employeeId(record.getEmployeeId())
                .employeeCode(record.getEmployeeCode())
                .checkType(record.getCheckType().name())
                .timestamp(record.getCheckTime())
                .locationId(record.getLocationId())
                .bssid(record.getWifiBssid())
                .gpsLat(record.getGpsLatitude())
                .gpsLng(record.getGpsLongitude())
                .deviceId(record.getDeviceId())
                .fraudScore(record.getFraudScore())
                .status(record.getStatus().name())
                .build());

        // 10. Build result
        String locationName = location != null ? location.getName() : "Unknown";
        String message = buildMessage(checkType, lateStatus, lateGraceRemaining);

        return new CheckInResult(
                record.getId(), record.getStatus().name(), locationName,
                record.getVerificationMethod().name(), record.getCheckTime(),
                record.getFraudScore(), message, lateStatus, lateGraceRemaining);
    }

    /**
     * 5-Scenario Verification Resolution.
     *
     * Scenario 1: WiFi BSSID match (primary)
     * Scenario 2: WiFi miss + GPS inside geofence (secondary)
     * Scenario 3: GPS only, no WiFi data
     * Scenario 4: QR code scan
     * Scenario 5: Manual entry (admin override)
     */
    private VerificationOutcome resolveVerification(CheckInCommand command) {
        // Scenario 5: Manual entry (admin override)
        if (command.isManualEntry() && command.manualLocationId() != null) {
            log.info("Scenario 5: Manual entry for employee {} at location {}",
                    command.employeeId(), command.manualLocationId());
            return new VerificationOutcome(
                    command.manualLocationId(),
                    AttendanceRecord.VerificationMethod.MANUAL,
                    null, null);
        }

        // Scenario 4: QR code scan
        if (command.qrToken() != null && !command.qrToken().isBlank()) {
            log.info("Scenario 4: QR code verification for employee {}", command.employeeId());
            UUID locationId = qrCodeUseCase.validateQrCode(command.qrToken());
            return new VerificationOutcome(
                    locationId,
                    AttendanceRecord.VerificationMethod.QR,
                    null, null);
        }

        // Scenarios 1-3: WiFi and/or GPS based
        boolean hasWifi = command.bssidSignals() != null && !command.bssidSignals().isEmpty();
        boolean hasGps = command.gpsCoordinate() != null;

        if (hasWifi) {
            try {
                // Try WiFi first
                LocationVerifier.VerificationResult wifiResult =
                        locationVerifier.verify(command.bssidSignals(), null);

                if (wifiResult.locationId() != null) {
                    // Scenario 1: WiFi BSSID matched
                    log.info("Scenario 1: WiFi BSSID match for employee {}", command.employeeId());
                    return new VerificationOutcome(
                            wifiResult.locationId(),
                            AttendanceRecord.VerificationMethod.WIFI,
                            wifiResult.bssid(), wifiResult.ssid());
                }
            } catch (InvalidLocationException e) {
                // WiFi verification failed, fall through to GPS
                log.debug("WiFi verification failed, trying GPS fallback: {}", e.getMessage());
            }

            // Scenario 2: WiFi miss + GPS inside geofence
            if (hasGps) {
                log.info("Scenario 2: WiFi miss + GPS fallback for employee {}", command.employeeId());
                LocationVerifier.VerificationResult gpsResult =
                        locationVerifier.verify(null, command.gpsCoordinate());
                return new VerificationOutcome(
                        gpsResult.locationId(),
                        AttendanceRecord.VerificationMethod.GPS,
                        null, null);
            }

            throw new InvalidLocationException(
                    "Không thể xác minh vị trí: WiFi không khớp và không có dữ liệu GPS");
        }

        // Scenario 3: GPS only (no WiFi data at all)
        if (hasGps) {
            log.info("Scenario 3: GPS-only verification for employee {}", command.employeeId());
            LocationVerifier.VerificationResult gpsResult =
                    locationVerifier.verify(null, command.gpsCoordinate());
            return new VerificationOutcome(
                    gpsResult.locationId(),
                    AttendanceRecord.VerificationMethod.GPS,
                    null, null);
        }

        throw new InvalidLocationException(
                "Không thể xác minh vị trí: không có dữ liệu WiFi, GPS hoặc QR");
    }

    /**
     * Handle late grace quota logic:
     * - If within grace period and quota remaining -> "trễ có phép", use quota
     * - If within grace period but quota exhausted -> "trễ không phép"
     * - If outside grace period -> "trễ không phép" (grace quota doesn't apply)
     */
    private LateGraceResult handleLateGraceQuota(
            UUID employeeId, Instant checkTime, ShiftEvaluation evaluation) {

        ZonedDateTime zdt = checkTime.atZone(VN_ZONE);
        int year = zdt.getYear();
        int month = zdt.getMonthValue();

        // Only within-grace-period lateness can consume quota
        if (!evaluation.isWithinGrace()) {
            log.info("Employee {} is late {} min (outside grace period) -> trễ không phép",
                    employeeId, evaluation.lateMinutes());
            // Still return remaining count for UI display
            int remaining = lateGraceQuotaRepository
                    .findByEmployeeAndMonth(employeeId, year, month)
                    .map(LateGraceQuota::getRemainingCount)
                    .orElse(0);
            return new LateGraceResult("TRE_KHONG_PHEP", remaining);
        }

        // Within grace period: check and use quota
        LateGraceQuota quota = lateGraceQuotaRepository
                .findByEmployeeAndMonth(employeeId, year, month)
                .orElseGet(() -> createDefaultQuota(employeeId, year, month));

        if (quota.hasRemaining()) {
            quota.use();
            lateGraceQuotaRepository.save(quota);
            int remaining = quota.getRemainingCount();

            log.info("Employee {} used late grace quota ({} remaining) -> trễ có phép",
                    employeeId, remaining);
            return new LateGraceResult("TRE_CO_PHEP", remaining);
        } else {
            log.info("Employee {} late grace quota exhausted -> trễ không phép", employeeId);
            return new LateGraceResult("TRE_KHONG_PHEP", 0);
        }
    }

    private LateGraceQuota createDefaultQuota(UUID employeeId, int year, int month) {
        // Resolve max allowed from config cascade
        int maxAllowed;
        try {
            maxAllowed = configResolver.getShiftConfig(employeeId).getLateGraceQuotaPerMonth();
        } catch (Exception e) {
            maxAllowed = DEFAULT_LATE_GRACE_MAX;
        }

        LateGraceQuota quota = LateGraceQuota.builder()
                .id(UUID.randomUUID())
                .employeeId(employeeId)
                .year(year)
                .month(month)
                .maxAllowed(maxAllowed)
                .usedCount(0)
                .build();
        return lateGraceQuotaRepository.save(quota);
    }

    private String buildMessage(AttendanceRecord.CheckType checkType,
                                String lateStatus, Integer remaining) {
        StringBuilder sb = new StringBuilder();
        if (checkType == AttendanceRecord.CheckType.CHECK_IN) {
            sb.append("Chấm công vào thành công");
        } else {
            sb.append("Chấm công ra thành công");
        }

        if ("TRE_CO_PHEP".equals(lateStatus)) {
            sb.append(". Trễ có phép");
            if (remaining != null) {
                if (remaining == 0) {
                    sb.append(" (đã hết lượt miễn trừ!)");
                } else if (remaining == 1) {
                    sb.append(" (còn 1 lượt miễn trừ)");
                } else {
                    sb.append(String.format(" (còn %d lượt miễn trừ)", remaining));
                }
            }
        } else if ("TRE_KHONG_PHEP".equals(lateStatus)) {
            sb.append(". Trễ không phép");
        }

        return sb.toString();
    }

    private record VerificationOutcome(
            UUID locationId,
            AttendanceRecord.VerificationMethod method,
            String bssid,
            String ssid
    ) {}

    private record LateGraceResult(String lateStatus, int remaining) {}
}
