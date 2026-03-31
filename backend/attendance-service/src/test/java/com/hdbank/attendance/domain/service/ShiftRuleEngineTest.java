package com.hdbank.attendance.domain.service;

import com.hdbank.attendance.domain.model.AttendanceRecord;
import com.hdbank.attendance.domain.model.Shift;
import com.hdbank.attendance.domain.valueobject.ShiftEvaluation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShiftRuleEngineTest {

    private ShiftRuleEngine engine;
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @BeforeEach
    void setUp() {
        engine = new ShiftRuleEngine();
    }

    private AttendanceRecord buildCheckIn(LocalTime time) {
        Instant instant = ZonedDateTime.of(2026, 3, 30, time.getHour(), time.getMinute(), 0, 0, VN_ZONE).toInstant();
        return AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .checkType(AttendanceRecord.CheckType.CHECK_IN)
                .checkTime(instant)
                .build();
    }

    private AttendanceRecord buildCheckOut(LocalTime time) {
        Instant instant = ZonedDateTime.of(2026, 3, 30, time.getHour(), time.getMinute(), 0, 0, VN_ZONE).toInstant();
        return AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .checkType(AttendanceRecord.CheckType.CHECK_OUT)
                .checkTime(instant)
                .build();
    }

    private Shift buildNormalShift() {
        return Shift.builder()
                .id(UUID.randomUUID())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .isOvernight(false)
                .gracePeriodMinutes(15)
                .earlyDepartureMinutes(15)
                .otMultiplier(BigDecimal.valueOf(1.5))
                .build();
    }

    private Shift buildOvernightShift() {
        return Shift.builder()
                .id(UUID.randomUUID())
                .startTime(LocalTime.of(21, 0))
                .endTime(LocalTime.of(5, 0))
                .isOvernight(true)
                .gracePeriodMinutes(15)
                .earlyDepartureMinutes(15)
                .otMultiplier(BigDecimal.valueOf(2.0))
                .build();
    }

    @Test
    @DisplayName("null record or shift returns onTime")
    void nullInputs_returnsOnTime() {
        ShiftEvaluation result = engine.evaluate(null, buildNormalShift());
        assertFalse(result.isLate());
        assertFalse(result.isEarly());

        result = engine.evaluate(buildCheckIn(LocalTime.of(8, 0)), null);
        assertFalse(result.isLate());
    }

    @Nested
    @DisplayName("Check-In Evaluation")
    class CheckInTests {

        @Test
        @DisplayName("on-time check-in at exactly shift start")
        void onTimeCheckIn() {
            ShiftEvaluation result = engine.evaluate(buildCheckIn(LocalTime.of(8, 0)), buildNormalShift());
            assertFalse(result.isLate());
            assertEquals(0, result.lateMinutes());
        }

        @Test
        @DisplayName("early arrival check-in before shift start")
        void earlyArrival() {
            ShiftEvaluation result = engine.evaluate(buildCheckIn(LocalTime.of(7, 45)), buildNormalShift());
            assertFalse(result.isLate());
        }

        @Test
        @DisplayName("late check-in within grace period (10 min late, grace=15)")
        void lateWithinGrace() {
            ShiftEvaluation result = engine.evaluate(buildCheckIn(LocalTime.of(8, 10)), buildNormalShift());
            assertTrue(result.isLate());
            assertTrue(result.isWithinGrace());
        }

        @Test
        @DisplayName("late check-in past grace period (20 min late, grace=15)")
        void latePastGrace() {
            ShiftEvaluation result = engine.evaluate(buildCheckIn(LocalTime.of(8, 20)), buildNormalShift());
            assertTrue(result.isLate());
            assertFalse(result.isWithinGrace());
        }

        @Test
        @DisplayName("late check-in uses default grace when shift grace is 0")
        void defaultGracePeriod() {
            Shift shift = buildNormalShift();
            shift.setGracePeriodMinutes(0);
            // 10 min late, default grace=15 -> within grace
            ShiftEvaluation result = engine.evaluate(buildCheckIn(LocalTime.of(8, 10)), shift);
            assertTrue(result.isLate());
            assertTrue(result.isWithinGrace());
        }

        @Test
        @DisplayName("rounding: 7 min late rounds to 0")
        void rounding_7min_roundsDown() {
            ShiftEvaluation result = engine.evaluate(buildCheckIn(LocalTime.of(8, 7)), buildNormalShift());
            // 7 min late, rounds to 0 (remainder 7 < 8 threshold)
            assertTrue(result.isLate());
            assertEquals(0, result.lateMinutes());
        }

        @Test
        @DisplayName("rounding: 8 min late rounds to 15")
        void rounding_8min_roundsUp() {
            ShiftEvaluation result = engine.evaluate(buildCheckIn(LocalTime.of(8, 8)), buildNormalShift());
            assertTrue(result.isLate());
            assertEquals(15, result.lateMinutes());
        }

        @Test
        @DisplayName("rounding: 22 min late rounds to 15")
        void rounding_22min_roundsDown() {
            ShiftEvaluation result = engine.evaluate(buildCheckIn(LocalTime.of(8, 22)), buildNormalShift());
            assertTrue(result.isLate());
            assertEquals(15, result.lateMinutes());
        }

        @Test
        @DisplayName("rounding: 23 min late rounds to 30")
        void rounding_23min_roundsUp() {
            ShiftEvaluation result = engine.evaluate(buildCheckIn(LocalTime.of(8, 23)), buildNormalShift());
            assertTrue(result.isLate());
            assertEquals(30, result.lateMinutes());
        }
    }

    @Nested
    @DisplayName("Check-Out Evaluation")
    class CheckOutTests {

        @Test
        @DisplayName("on-time check-out at exactly shift end")
        void onTimeCheckOut() {
            ShiftEvaluation result = engine.evaluate(buildCheckOut(LocalTime.of(17, 0)), buildNormalShift());
            assertFalse(result.isEarly());
            assertFalse(result.isOvertimeEligible());
        }

        @Test
        @DisplayName("early departure before shift end")
        void earlyDeparture() {
            ShiftEvaluation result = engine.evaluate(buildCheckOut(LocalTime.of(16, 30)), buildNormalShift());
            assertTrue(result.isEarly());
            assertEquals(30, result.earlyMinutes());
        }

        @Test
        @DisplayName("overtime when checking out after shift end")
        void overtime() {
            ShiftEvaluation result = engine.evaluate(buildCheckOut(LocalTime.of(18, 0)), buildNormalShift());
            assertFalse(result.isEarly());
            assertTrue(result.isOvertimeEligible());
            assertEquals(60, result.otMinutes());
        }

        @Test
        @DisplayName("OT not eligible when shift has no OT multiplier")
        void noOtMultiplier() {
            Shift shift = buildNormalShift();
            shift.setOtMultiplier(null);
            ShiftEvaluation result = engine.evaluate(buildCheckOut(LocalTime.of(18, 0)), shift);
            assertFalse(result.isOvertimeEligible());
        }
    }

    @Nested
    @DisplayName("Overnight Shift")
    class OvernightShiftTests {

        @Test
        @DisplayName("on-time check-in for overnight shift (21:00 start)")
        void onTimeOvernightCheckIn() {
            ShiftEvaluation result = engine.evaluate(buildCheckIn(LocalTime.of(20, 50)), buildOvernightShift());
            assertFalse(result.isLate());
        }

        @Test
        @DisplayName("late check-in for overnight shift (21:15)")
        void lateOvernightCheckIn() {
            ShiftEvaluation result = engine.evaluate(buildCheckIn(LocalTime.of(21, 15)), buildOvernightShift());
            assertTrue(result.isLate());
            assertTrue(result.isWithinGrace());
        }

        @Test
        @DisplayName("early departure for overnight shift (check-out at 04:50, end 05:00)")
        void earlyDepartureOvernight() {
            // Build check-out for next day morning
            Instant instant = ZonedDateTime.of(2026, 3, 31, 4, 50, 0, 0, VN_ZONE).toInstant();
            AttendanceRecord record = AttendanceRecord.builder()
                    .id(UUID.randomUUID())
                    .checkType(AttendanceRecord.CheckType.CHECK_OUT)
                    .checkTime(instant)
                    .build();
            ShiftEvaluation result = engine.evaluate(record, buildOvernightShift());
            assertTrue(result.isEarly());
        }

        @Test
        @DisplayName("overtime for overnight shift (check-out at 05:30, end 05:00)")
        void overtimeOvernight() {
            Instant instant = ZonedDateTime.of(2026, 3, 31, 5, 30, 0, 0, VN_ZONE).toInstant();
            AttendanceRecord record = AttendanceRecord.builder()
                    .id(UUID.randomUUID())
                    .checkType(AttendanceRecord.CheckType.CHECK_OUT)
                    .checkTime(instant)
                    .build();
            ShiftEvaluation result = engine.evaluate(record, buildOvernightShift());
            assertTrue(result.isOvertimeEligible());
            assertEquals(30, result.otMinutes());
        }
    }
}
