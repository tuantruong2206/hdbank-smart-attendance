package com.hdbank.attendance.domain.service;

import com.hdbank.attendance.domain.model.AttendanceRecord;
import com.hdbank.attendance.domain.model.Shift;
import com.hdbank.attendance.domain.valueobject.ShiftEvaluation;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Pure domain service that evaluates an attendance record against a shift.
 * Handles normal shifts, overnight shifts (cross-midnight), grace periods,
 * early departure, overtime, and rounding rules.
 */
public class ShiftRuleEngine {

    private static final int DEFAULT_GRACE_PERIOD_MINUTES = 15;
    private static final int DEFAULT_EARLY_DEPARTURE_MINUTES = 15;
    private static final int ROUNDING_INTERVAL_MINUTES = 15;

    /**
     * Evaluate a CHECK_IN record against its assigned shift.
     * Returns shift evaluation with late/early/OT status.
     */
    public ShiftEvaluation evaluate(AttendanceRecord record, Shift shift) {
        if (record == null || shift == null) {
            return ShiftEvaluation.onTime();
        }

        if (record.getCheckType() == AttendanceRecord.CheckType.CHECK_IN) {
            return evaluateCheckIn(record, shift);
        } else {
            return evaluateCheckOut(record, shift);
        }
    }

    private ShiftEvaluation evaluateCheckIn(AttendanceRecord record, Shift shift) {
        LocalTime checkTime = toLocalTime(record);
        LocalTime shiftStart = shift.getStartTime();

        int gracePeriod = shift.getGracePeriodMinutes() > 0
                ? shift.getGracePeriodMinutes()
                : DEFAULT_GRACE_PERIOD_MINUTES;

        long lateMinutes = computeLateMinutesForCheckIn(checkTime, shiftStart, shift.isOvernight());

        if (lateMinutes <= 0) {
            // On time or early arrival
            return ShiftEvaluation.onTime();
        }

        long roundedLate = roundToNearest(lateMinutes);
        boolean withinGrace = lateMinutes <= gracePeriod;

        return ShiftEvaluation.late(roundedLate, withinGrace);
    }

    private ShiftEvaluation evaluateCheckOut(AttendanceRecord record, Shift shift) {
        LocalTime checkTime = toLocalTime(record);
        LocalTime shiftEnd = shift.getEndTime();

        int earlyThreshold = shift.getEarlyDepartureMinutes() > 0
                ? shift.getEarlyDepartureMinutes()
                : DEFAULT_EARLY_DEPARTURE_MINUTES;

        long earlyMinutes = computeEarlyMinutesForCheckOut(checkTime, shiftEnd, shift.isOvernight());

        if (earlyMinutes > 0) {
            // Left early
            long roundedEarly = roundToNearest(earlyMinutes);
            boolean isSignificantEarly = earlyMinutes > earlyThreshold;
            return ShiftEvaluation.earlyDeparture(roundedEarly, false, 0);
        }

        // Stayed on time or overtime
        long overtimeMinutes = -earlyMinutes; // negative earlyMinutes means stayed late
        if (overtimeMinutes > 0) {
            long roundedOt = roundToNearest(overtimeMinutes);
            boolean otEligible = shift.getOtMultiplier() != null
                    && shift.getOtMultiplier().doubleValue() > 0;
            return new ShiftEvaluation(false, false, 0, 0, false, otEligible, roundedOt);
        }

        return ShiftEvaluation.onTime();
    }

    /**
     * Compute how many minutes late the check-in is relative to shift start.
     * Handles overnight shifts where start time is after midnight boundary.
     *
     * @return positive if late, negative or zero if on time/early
     */
    private long computeLateMinutesForCheckIn(LocalTime checkTime, LocalTime shiftStart, boolean isOvernight) {
        if (isOvernight) {
            // Overnight shift: e.g., shift starts at 21:00
            // Check-in at 20:50 -> on time (10 min early)
            // Check-in at 21:15 -> 15 min late
            // Check-in at 22:00 -> 60 min late
            // Check-in before shift start (same evening) is on time
            return minutesBetweenWithOvernight(shiftStart, checkTime, true);
        } else {
            return Duration.between(shiftStart, checkTime).toMinutes();
        }
    }

    /**
     * Compute how many minutes early the check-out is relative to shift end.
     * Handles overnight shifts where end time is after midnight.
     *
     * @return positive if early departure, negative if stayed late (overtime)
     */
    private long computeEarlyMinutesForCheckOut(LocalTime checkTime, LocalTime shiftEnd, boolean isOvernight) {
        if (isOvernight) {
            // Overnight shift: e.g., shift ends at 05:00
            // Check-out at 05:10 -> 10 min overtime (normal, not early)
            // Check-out at 04:50 -> 10 min early
            return minutesBetweenWithOvernight(checkTime, shiftEnd, false);
        } else {
            return Duration.between(checkTime, shiftEnd).toMinutes();
        }
    }

    /**
     * Calculate signed minutes between two times accounting for overnight wraparound.
     *
     * For check-in evaluation (isCheckIn=true):
     *   shiftStart=21:00, checkTime=21:15 -> +15 (late)
     *   shiftStart=21:00, checkTime=20:50 -> -10 (early/on-time)
     *
     * For check-out evaluation (isCheckIn=false):
     *   checkTime=04:50, shiftEnd=05:00 -> +10 (early departure)
     *   checkTime=05:10, shiftEnd=05:00 -> -10 (overtime)
     */
    private long minutesBetweenWithOvernight(LocalTime from, LocalTime to, boolean isCheckIn) {
        long minutes = Duration.between(from, to).toMinutes();

        if (isCheckIn) {
            // For check-in against overnight shift start:
            // If check time appears to be "before" start but the diff is huge negative
            // (e.g., checkTime=20:50, shiftStart=21:00 => -10 which is correct: early)
            // If checkTime=02:00, shiftStart=21:00 => Duration gives +300 but actually very late
            // We need to handle the case where someone checks in after midnight for a pre-midnight shift
            if (minutes < -720) {
                // Check-in after midnight for a shift that started before midnight
                minutes += 1440;
            } else if (minutes > 720) {
                // Check-in well before shift (next day interpretation not needed)
                minutes -= 1440;
            }
        } else {
            // For check-out against overnight shift end (end is typically early morning):
            // checkTime=04:50, shiftEnd=05:00 => +10 (early, correct)
            // checkTime=05:10, shiftEnd=05:00 => -10 (overtime, correct)
            // checkTime=23:00, shiftEnd=05:00 => Duration gives +360, but actually left very early
            if (minutes < -720) {
                minutes += 1440;
            } else if (minutes > 720) {
                // Left before midnight for a shift ending after midnight -> very early
                // Keep as-is, this is a large early departure
            }
        }

        return minutes;
    }

    /**
     * Round minutes to nearest ROUNDING_INTERVAL_MINUTES (15 min).
     * e.g., 7 min -> 0, 8 min -> 15, 22 min -> 15, 23 min -> 30
     */
    private long roundToNearest(long minutes) {
        if (minutes <= 0) return 0;
        long remainder = minutes % ROUNDING_INTERVAL_MINUTES;
        if (remainder < (ROUNDING_INTERVAL_MINUTES / 2 + 1)) {
            return minutes - remainder;
        } else {
            return minutes - remainder + ROUNDING_INTERVAL_MINUTES;
        }
    }

    private LocalTime toLocalTime(AttendanceRecord record) {
        return record.getCheckTime()
                .atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .toLocalTime();
    }
}
