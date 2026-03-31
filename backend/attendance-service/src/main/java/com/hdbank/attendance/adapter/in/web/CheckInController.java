package com.hdbank.attendance.adapter.in.web;

import com.hdbank.attendance.adapter.in.web.request.CheckInRequest;
import com.hdbank.attendance.application.dto.CheckInCommand;
import com.hdbank.attendance.application.dto.CheckInResult;
import com.hdbank.attendance.application.port.in.CheckInUseCase;
import com.hdbank.attendance.application.port.in.CheckOutUseCase;
import com.hdbank.attendance.application.port.out.AttendanceRepository;
import com.hdbank.attendance.domain.model.AttendanceRecord;
import com.hdbank.attendance.domain.valueobject.BssidSignal;
import com.hdbank.attendance.domain.valueobject.GpsCoordinate;
import com.hdbank.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInUseCase checkInUseCase;
    private final CheckOutUseCase checkOutUseCase;
    private final AttendanceRepository attendanceRepository;

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<CheckInResult>> checkIn(
            @Valid @RequestBody CheckInRequest request,
            @RequestHeader("X-User-Id") String userId) {
        CheckInCommand command = toCommand(request, UUID.fromString(userId));
        CheckInResult result = checkInUseCase.checkIn(command);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/check-out")
    public ResponseEntity<ApiResponse<CheckInResult>> checkOut(
            @Valid @RequestBody CheckInRequest request,
            @RequestHeader("X-User-Id") String userId) {
        CheckInCommand command = toCommand(request, UUID.fromString(userId));
        CheckInResult result = checkOutUseCase.checkOut(command);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<AttendanceRecord>>> getToday(
            @RequestHeader("X-User-Id") String userId) {
        UUID employeeId = UUID.fromString(userId);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Instant start = today.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        List<AttendanceRecord> records = attendanceRepository.findByEmployeeAndTimeRange(employeeId, start, end);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AttendanceRecord>>> getHistory(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String from,
            @RequestParam String to) {
        UUID employeeId = UUID.fromString(userId);
        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant start = LocalDate.parse(from).atStartOfDay(vnZone).toInstant();
        Instant end = LocalDate.parse(to).plusDays(1).atStartOfDay(vnZone).toInstant();
        List<AttendanceRecord> records = attendanceRepository.findByEmployeeAndTimeRange(employeeId, start, end);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    private CheckInCommand toCommand(CheckInRequest req, UUID userId) {
        List<BssidSignal> signals = req.bssidSignals() != null
                ? req.bssidSignals().stream()
                    .map(s -> new BssidSignal(s.bssid(), s.rssi()))
                    .toList()
                : List.of();

        GpsCoordinate gps = req.gpsLatitude() != null
                ? new GpsCoordinate(req.gpsLatitude(), req.gpsLongitude(), req.gpsAccuracy() != null ? req.gpsAccuracy() : 0)
                : null;

        return new CheckInCommand(
                userId, req.employeeCode(), req.employeeType(),
                signals, gps, req.deviceId(), req.deviceInfo(),
                req.isOffline() != null && req.isOffline(),
                req.offlineUuid(), req.offlineTimestamp(),
                req.qrToken(),
                req.isManualEntry() != null && req.isManualEntry(),
                req.manualLocationId(),
                req.manualReason());
    }
}
