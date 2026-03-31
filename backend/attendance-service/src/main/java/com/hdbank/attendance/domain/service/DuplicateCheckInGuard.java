package com.hdbank.attendance.domain.service;

import com.hdbank.attendance.domain.exception.DuplicateCheckInException;
import com.hdbank.attendance.domain.model.AttendanceRecord;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class DuplicateCheckInGuard {

    private static final Duration IT_MIN_INTERVAL = Duration.ofMinutes(30);

    private final Function<UUID, Optional<AttendanceRecord>> lastCheckInLookup;

    public DuplicateCheckInGuard(Function<UUID, Optional<AttendanceRecord>> lastCheckInLookup) {
        this.lastCheckInLookup = lastCheckInLookup;
    }

    /**
     * Check for duplicate check-in/check-out.
     * @param checkType CHECK_IN or CHECK_OUT — enforces rules for both
     */
    public void check(UUID employeeId, String employeeType, UUID shiftId, Instant checkTime,
                       AttendanceRecord.CheckType checkType) {
        Optional<AttendanceRecord> lastRecord = lastCheckInLookup.apply(employeeId);
        if (lastRecord.isEmpty()) return;

        AttendanceRecord last = lastRecord.get();

        if ("NGHIEP_VU".equals(employeeType)) {
            // Nghiệp vụ: max 1 CHECK_IN + 1 CHECK_OUT per day
            if (last.getCheckType() == checkType && isSameDay(last.getCheckTime(), checkTime)) {
                String label = checkType == AttendanceRecord.CheckType.CHECK_IN ? "vào" : "ra";
                throw new DuplicateCheckInException(
                        "Nhân viên nghiệp vụ chỉ được chấm công " + label + " 1 lần/ca. Bạn đã chấm công " + label + " hôm nay.");
            }
            // Cannot check-out without checking in first
            if (checkType == AttendanceRecord.CheckType.CHECK_OUT
                    && last.getCheckType() != AttendanceRecord.CheckType.CHECK_IN) {
                // No CHECK_IN today yet — last record is something else
                throw new DuplicateCheckInException(
                        "Bạn chưa chấm công vào hôm nay. Vui lòng chấm công vào trước.");
            }
        } else {
            // IT: configurable interval (default 30 min) between ANY records
            Duration elapsed = Duration.between(last.getCheckTime(), checkTime);
            if (elapsed.compareTo(IT_MIN_INTERVAL) < 0) {
                long remainingMin = IT_MIN_INTERVAL.minus(elapsed).toMinutes() + 1;
                throw new DuplicateCheckInException(
                        "Chấm công quá nhanh. Vui lòng đợi " + remainingMin + " phút nữa.");
            }
        }
    }

    /**
     * Backward-compatible overload (defaults to CHECK_IN).
     */
    public void check(UUID employeeId, String employeeType, UUID shiftId, Instant checkTime) {
        check(employeeId, employeeType, shiftId, checkTime, AttendanceRecord.CheckType.CHECK_IN);
    }

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private boolean isSameDay(Instant a, Instant b) {
        LocalDate dayA = a.atZone(VN_ZONE).toLocalDate();
        LocalDate dayB = b.atZone(VN_ZONE).toLocalDate();
        return dayA.equals(dayB);
    }
}
