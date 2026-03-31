package com.hdbank.attendance.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Pure domain service for generating QR code tokens.
 * Token = SHA-256(UUID + timestamp + locationId), TTL 5-10 min.
 */
public class QrCodeGenerator {

    private static final int DEFAULT_TTL_SECONDS = 300; // 5 minutes
    private static final int MAX_TTL_SECONDS = 600; // 10 minutes

    /**
     * Generate a unique one-time QR token for a location.
     *
     * @param locationId the location this QR code is for
     * @return a QrToken containing the token string and TTL
     */
    public QrToken generate(UUID locationId) {
        return generate(locationId, DEFAULT_TTL_SECONDS);
    }

    /**
     * Generate a unique one-time QR token with a custom TTL.
     *
     * @param locationId the location this QR code is for
     * @param ttlSeconds TTL in seconds (clamped to 5-10 minutes)
     * @return a QrToken containing the token string and TTL
     */
    public QrToken generate(UUID locationId, int ttlSeconds) {
        int clampedTtl = Math.max(DEFAULT_TTL_SECONDS, Math.min(ttlSeconds, MAX_TTL_SECONDS));

        String raw = UUID.randomUUID() + ":" + Instant.now().toEpochMilli() + ":" + locationId;
        String token = sha256(raw);

        Instant expiresAt = Instant.now().plusSeconds(clampedTtl);

        return new QrToken(token, locationId, expiresAt, clampedTtl);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record QrToken(
            String token,
            UUID locationId,
            Instant expiresAt,
            int ttlSeconds
    ) {}
}
