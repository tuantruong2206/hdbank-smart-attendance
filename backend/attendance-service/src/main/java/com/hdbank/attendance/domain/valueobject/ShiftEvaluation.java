package com.hdbank.attendance.domain.valueobject;

public record ShiftEvaluation(
        boolean isLate,
        boolean isEarly,
        long lateMinutes,
        long earlyMinutes,
        boolean isWithinGrace,
        boolean isOvertimeEligible,
        long otMinutes
) {

    public static ShiftEvaluation onTime() {
        return new ShiftEvaluation(false, false, 0, 0, false, false, 0);
    }

    public static ShiftEvaluation late(long lateMinutes, boolean withinGrace) {
        return new ShiftEvaluation(true, false, lateMinutes, 0, withinGrace, false, 0);
    }

    public static ShiftEvaluation earlyDeparture(long earlyMinutes, boolean otEligible, long otMinutes) {
        return new ShiftEvaluation(false, true, 0, earlyMinutes, false, otEligible, otMinutes);
    }

    public static ShiftEvaluation overtime(long otMinutes) {
        return new ShiftEvaluation(false, false, 0, 0, false, true, otMinutes);
    }
}
