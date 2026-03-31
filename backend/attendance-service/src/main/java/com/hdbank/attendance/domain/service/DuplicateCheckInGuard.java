package com.hdbank.attendance.domain.service;

import com.hdbank.attendance.domain.exception.DuplicateCheckInException;
import com.hdbank.attendance.domain.model.AttendanceRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class DuplicateCheckInGuard {

    private static final Duration IT_MIN_INTERVAL = Duration.ofMinutes(30);

    private final Function<UUID, Optional<AttendanceRecord>> lastCheckInLookup;

    public DuplicateCheckInGuard(Function<UUID, Optional<AttendanceRecord>> lastCheckInLookup) {
        this.lastCheckInLookup = lastCheckInLookup;
    }

    public void check(UUID employeeId, String employeeType, UUID shiftId, Instant checkTime) {
        Optional<AttendanceRecord> lastRecord = lastCheckInLookup.apply(employeeId);
        if (lastRecord.isEmpty()) return;

        AttendanceRecord last = lastRecord.get();

        if ("NGHIEP_VU".equals(employeeType)) {
            // Max 1 check-in per shift
            if (last.getShiftId() != null && last.getShiftId().equals(shiftId)
                    && last.getCheckType() == AttendanceRecord.CheckType.CHECK_IN) {
                throw new DuplicateCheckInException(
                        "Nhân viên nghiệp vụ chỉ được chấm công 1 lần/ca");
            }
        } else {
            // IT: configurable interval (default 30 min)
            Duration elapsed = Duration.between(last.getCheckTime(), checkTime);
            if (elapsed.compareTo(IT_MIN_INTERVAL) < 0) {
                throw new DuplicateCheckInException(
                        "Chấm công quá nhanh. Vui lòng đợi " +
                                IT_MIN_INTERVAL.toMinutes() + " phút giữa các lần chấm công");
            }
        }
    }
}
