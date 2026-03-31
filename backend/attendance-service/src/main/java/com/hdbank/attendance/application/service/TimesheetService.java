package com.hdbank.attendance.application.service;

import com.hdbank.attendance.application.port.in.ManageTimesheetUseCase;
import com.hdbank.attendance.application.port.out.AttendanceRepository;
import com.hdbank.attendance.application.port.out.EmployeeRepository;
import com.hdbank.attendance.application.port.out.TimesheetRepository;
import com.hdbank.attendance.domain.model.AttendanceRecord;
import com.hdbank.attendance.domain.model.Timesheet;
import com.hdbank.attendance.domain.model.Timesheet.TimesheetStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimesheetService implements ManageTimesheetUseCase {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalTime STANDARD_START = LocalTime.of(8, 0);
    private static final LocalTime STANDARD_END = LocalTime.of(17, 0);
    private static final int GRACE_PERIOD_MINUTES = 15;

    private final TimesheetRepository timesheetRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public Timesheet getOrCreateTimesheet(UUID employeeId, int month, int year) {
        return timesheetRepository.findByEmployeeAndMonthYear(employeeId, month, year)
                .orElseGet(() -> {
                    Timesheet timesheet = Timesheet.builder()
                            .id(UUID.randomUUID())
                            .employeeId(employeeId)
                            .month(month)
                            .year(year)
                            .status(TimesheetStatus.DRAFT)
                            .totalWorkDays(0)
                            .lateCount(0)
                            .earlyLeaveCount(0)
                            .absentDays(0)
                            .otHours(BigDecimal.ZERO)
                            .lateGraceUsed(0)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return timesheetRepository.save(timesheet);
                });
    }

    @Override
    @Transactional
    public Timesheet calculateTimesheet(UUID employeeId, int month, int year) {
        Timesheet timesheet = getOrCreateTimesheet(employeeId, month, year);

        if (timesheet.getStatus() == TimesheetStatus.LOCKED) {
            throw new IllegalStateException("Không thể tính lại bảng công đã khóa");
        }

        // Query attendance records for the period
        YearMonth yearMonth = YearMonth.of(year, month);
        Instant periodStart = yearMonth.atDay(1).atStartOfDay(VN_ZONE).toInstant();
        Instant periodEnd = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(VN_ZONE).toInstant();

        List<AttendanceRecord> records = attendanceRepository
                .findByEmployeeAndTimeRange(employeeId, periodStart, periodEnd);

        // Group records by date
        Map<LocalDate, List<AttendanceRecord>> recordsByDate = new LinkedHashMap<>();
        for (AttendanceRecord record : records) {
            if (record.getStatus() == AttendanceRecord.Status.REJECTED) {
                continue;
            }
            LocalDate date = record.getCheckTime().atZone(VN_ZONE).toLocalDate();
            recordsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(record);
        }

        int workDays = 0;
        int lateCount = 0;
        int earlyLeaveCount = 0;
        int lateGraceUsed = 0;
        BigDecimal totalOtHours = BigDecimal.ZERO;

        // Calculate working days in the month (Mon-Fri)
        int totalBusinessDays = 0;
        LocalDate current = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        while (!current.isAfter(endDate)) {
            int dow = current.getDayOfWeek().getValue();
            if (dow <= 5) { // Mon-Fri
                totalBusinessDays++;
            }
            current = current.plusDays(1);
        }

        for (Map.Entry<LocalDate, List<AttendanceRecord>> entry : recordsByDate.entrySet()) {
            List<AttendanceRecord> dayRecords = entry.getValue();

            // Find first CHECK_IN and last CHECK_OUT for the day
            AttendanceRecord firstCheckIn = dayRecords.stream()
                    .filter(r -> r.getCheckType() == AttendanceRecord.CheckType.CHECK_IN)
                    .min(Comparator.comparing(AttendanceRecord::getCheckTime))
                    .orElse(null);

            AttendanceRecord lastCheckOut = dayRecords.stream()
                    .filter(r -> r.getCheckType() == AttendanceRecord.CheckType.CHECK_OUT)
                    .max(Comparator.comparing(AttendanceRecord::getCheckTime))
                    .orElse(null);

            if (firstCheckIn != null) {
                workDays++;

                // Check late
                LocalTime checkInTime = firstCheckIn.getCheckTime().atZone(VN_ZONE).toLocalTime();
                if (checkInTime.isAfter(STANDARD_START.plusMinutes(GRACE_PERIOD_MINUTES))) {
                    lateCount++;
                } else if (checkInTime.isAfter(STANDARD_START)) {
                    // Within grace period — counts as "trễ có phép"
                    lateGraceUsed++;
                }

                // Check early leave
                if (lastCheckOut != null) {
                    LocalTime checkOutTime = lastCheckOut.getCheckTime().atZone(VN_ZONE).toLocalTime();
                    if (checkOutTime.isBefore(STANDARD_END)) {
                        earlyLeaveCount++;
                    }

                    // Calculate OT (time after 17:00)
                    if (checkOutTime.isAfter(STANDARD_END)) {
                        long otMinutes = ChronoUnit.MINUTES.between(STANDARD_END, checkOutTime);
                        totalOtHours = totalOtHours.add(
                                BigDecimal.valueOf(otMinutes).divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP));
                    }
                }
            }
        }

        int absentDays = totalBusinessDays - workDays;
        if (absentDays < 0) absentDays = 0;

        timesheet.setTotalWorkDays(workDays);
        timesheet.setLateCount(lateCount);
        timesheet.setEarlyLeaveCount(earlyLeaveCount);
        timesheet.setAbsentDays(absentDays);
        timesheet.setOtHours(totalOtHours);
        timesheet.setLateGraceUsed(lateGraceUsed);
        timesheet.setUpdatedAt(Instant.now());

        return timesheetRepository.save(timesheet);
    }

    @Override
    @Transactional
    public Timesheet submitForReview(UUID timesheetId) {
        Timesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy bảng công: " + timesheetId));
        timesheet.submitForReview();
        return timesheetRepository.save(timesheet);
    }

    @Override
    @Transactional
    public Timesheet approve(UUID timesheetId, UUID approverId) {
        Timesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy bảng công: " + timesheetId));
        timesheet.approve(approverId);
        return timesheetRepository.save(timesheet);
    }

    @Override
    @Transactional
    public Timesheet lock(UUID timesheetId, UUID lockerId) {
        Timesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy bảng công: " + timesheetId));

        // Create JSONB snapshot of all attendance records for the period
        YearMonth yearMonth = YearMonth.of(timesheet.getYear(), timesheet.getMonth());
        Instant periodStart = yearMonth.atDay(1).atStartOfDay(VN_ZONE).toInstant();
        Instant periodEnd = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(VN_ZONE).toInstant();

        List<AttendanceRecord> records = attendanceRepository
                .findByEmployeeAndTimeRange(timesheet.getEmployeeId(), periodStart, periodEnd);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("lockedAt", Instant.now().toString());
        snapshot.put("lockedBy", lockerId.toString());
        snapshot.put("totalWorkDays", timesheet.getTotalWorkDays());
        snapshot.put("lateCount", timesheet.getLateCount());
        snapshot.put("earlyLeaveCount", timesheet.getEarlyLeaveCount());
        snapshot.put("absentDays", timesheet.getAbsentDays());
        snapshot.put("otHours", timesheet.getOtHours().toString());
        snapshot.put("lateGraceUsed", timesheet.getLateGraceUsed());

        List<Map<String, Object>> recordSnapshots = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId().toString());
            m.put("checkType", r.getCheckType().name());
            m.put("checkTime", r.getCheckTime().toString());
            m.put("status", r.getStatus().name());
            m.put("verificationMethod", r.getVerificationMethod().name());
            m.put("fraudScore", r.getFraudScore());
            return m;
        }).toList();
        snapshot.put("records", recordSnapshots);

        timesheet.lock(lockerId, snapshot);
        return timesheetRepository.save(timesheet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Timesheet> getTimesheetsByManager(UUID managerId, int month, int year) {
        List<UUID> employeeIds = employeeRepository.findEmployeeIdsByManagerId(managerId);
        if (employeeIds.isEmpty()) {
            return List.of();
        }
        return timesheetRepository.findByEmployeeIds(employeeIds, month, year);
    }
}
