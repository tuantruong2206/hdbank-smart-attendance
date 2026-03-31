package com.hdbank.attendance.application.dto;

import java.time.Instant;
import java.util.UUID;

public record CheckInResult(
        UUID recordId,
        String status,
        String locationName,
        String verificationMethod,
        Instant checkTime,
        int fraudScore,
        String message,
        String lateStatus,
        Integer lateGraceRemaining
) {
    /** Backward-compatible constructor without late grace info */
    public CheckInResult(UUID recordId, String status, String locationName,
                         String verificationMethod, Instant checkTime,
                         int fraudScore, String message) {
        this(recordId, status, locationName, verificationMethod, checkTime,
                fraudScore, message, null, null);
    }
}
