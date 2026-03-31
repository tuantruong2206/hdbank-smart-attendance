package com.hdbank.attendance.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface QrCodeUseCase {

    /**
     * Generate a dynamic QR code for a location. TTL 5-10 minutes, one-time use.
     *
     * @param locationId the location to generate the QR code for
     * @return QR code result with token, expiry, and TTL
     */
    QrCodeResult generateQrCode(UUID locationId);

    /**
     * Validate a QR code token. Returns the locationId if valid.
     * Consumes the token (one-time use).
     *
     * @param token the QR code token to validate
     * @return the locationId if the token is valid
     * @throws IllegalArgumentException if the token is invalid or expired
     */
    UUID validateQrCode(String token);

    record QrCodeResult(
            String token,
            UUID locationId,
            Instant expiresAt,
            int ttlSeconds
    ) {}
}
