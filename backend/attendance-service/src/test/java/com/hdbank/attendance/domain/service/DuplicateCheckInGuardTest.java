package com.hdbank.attendance.domain.service;

import com.hdbank.attendance.domain.exception.DuplicateCheckInException;
import com.hdbank.attendance.domain.model.AttendanceRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateCheckInGuardTest {

    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID SHIFT_ID = UUID.randomUUID();

    private DuplicateCheckInGuard guardWithLastRecord(AttendanceRecord lastRecord) {
        return new DuplicateCheckInGuard(id -> Optional.ofNullable(lastRecord));
    }

    private DuplicateCheckInGuard guardWithNoRecord() {
        return new DuplicateCheckInGuard(id -> Optional.empty());
    }

    @Test
    @DisplayName("first check-in ever is allowed (no previous record)")
    void firstCheckIn_allowed() {
        DuplicateCheckInGuard guard = guardWithNoRecord();
        assertDoesNotThrow(() ->
                guard.check(EMPLOYEE_ID, "NGHIEP_VU", SHIFT_ID, Instant.now()));
    }

    @Test
    @DisplayName("nghiep_vu second check-in same shift throws DuplicateCheckInException")
    void nghiepVu_sameShift_throws() {
        AttendanceRecord lastRecord = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .employeeId(EMPLOYEE_ID)
                .shiftId(SHIFT_ID)
                .checkType(AttendanceRecord.CheckType.CHECK_IN)
                .checkTime(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        DuplicateCheckInGuard guard = guardWithLastRecord(lastRecord);

        assertThrows(DuplicateCheckInException.class, () ->
                guard.check(EMPLOYEE_ID, "NGHIEP_VU", SHIFT_ID, Instant.now()));
    }

    @Test
    @DisplayName("nghiep_vu check-in on different shift is allowed")
    void nghiepVu_differentShift_allowed() {
        UUID differentShiftId = UUID.randomUUID();
        AttendanceRecord lastRecord = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .employeeId(EMPLOYEE_ID)
                .shiftId(differentShiftId)
                .checkType(AttendanceRecord.CheckType.CHECK_IN)
                .checkTime(Instant.now().minus(8, ChronoUnit.HOURS))
                .build();

        DuplicateCheckInGuard guard = guardWithLastRecord(lastRecord);

        assertDoesNotThrow(() ->
                guard.check(EMPLOYEE_ID, "NGHIEP_VU", SHIFT_ID, Instant.now()));
    }

    @Test
    @DisplayName("nghiep_vu check-out after check-in on same shift is allowed")
    void nghiepVu_checkOutAfterCheckIn_allowed() {
        AttendanceRecord lastRecord = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .employeeId(EMPLOYEE_ID)
                .shiftId(SHIFT_ID)
                .checkType(AttendanceRecord.CheckType.CHECK_OUT) // last was checkout
                .checkTime(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        DuplicateCheckInGuard guard = guardWithLastRecord(lastRecord);

        assertDoesNotThrow(() ->
                guard.check(EMPLOYEE_ID, "NGHIEP_VU", SHIFT_ID, Instant.now()));
    }

    @Test
    @DisplayName("IT employee check-in within 30 min interval throws")
    void it_within30min_throws() {
        AttendanceRecord lastRecord = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .employeeId(EMPLOYEE_ID)
                .checkType(AttendanceRecord.CheckType.CHECK_IN)
                .checkTime(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();

        DuplicateCheckInGuard guard = guardWithLastRecord(lastRecord);

        assertThrows(DuplicateCheckInException.class, () ->
                guard.check(EMPLOYEE_ID, "IT_KY_THUAT", SHIFT_ID, Instant.now()));
    }

    @Test
    @DisplayName("IT employee check-in after 30 min interval is allowed")
    void it_after30min_allowed() {
        AttendanceRecord lastRecord = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .employeeId(EMPLOYEE_ID)
                .checkType(AttendanceRecord.CheckType.CHECK_IN)
                .checkTime(Instant.now().minus(31, ChronoUnit.MINUTES))
                .build();

        DuplicateCheckInGuard guard = guardWithLastRecord(lastRecord);

        assertDoesNotThrow(() ->
                guard.check(EMPLOYEE_ID, "IT_KY_THUAT", SHIFT_ID, Instant.now()));
    }

    @Test
    @DisplayName("IT employee check-in at exactly 30 min boundary is allowed")
    void it_exactly30min_allowed() {
        Instant now = Instant.now();
        AttendanceRecord lastRecord = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .employeeId(EMPLOYEE_ID)
                .checkType(AttendanceRecord.CheckType.CHECK_IN)
                .checkTime(now.minus(30, ChronoUnit.MINUTES))
                .build();

        DuplicateCheckInGuard guard = guardWithLastRecord(lastRecord);

        assertDoesNotThrow(() ->
                guard.check(EMPLOYEE_ID, "IT_KY_THUAT", SHIFT_ID, now));
    }
}
