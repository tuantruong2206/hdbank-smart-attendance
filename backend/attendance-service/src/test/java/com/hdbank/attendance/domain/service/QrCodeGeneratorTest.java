package com.hdbank.attendance.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeGeneratorTest {

    private QrCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new QrCodeGenerator();
    }

    @Test
    @DisplayName("generate returns non-null token")
    void generate_returnsNonNullToken() {
        UUID locationId = UUID.randomUUID();
        QrCodeGenerator.QrToken token = generator.generate(locationId);

        assertNotNull(token);
        assertNotNull(token.token());
        assertFalse(token.token().isBlank());
        assertEquals(locationId, token.locationId());
    }

    @Test
    @DisplayName("default TTL is 300 seconds (5 minutes)")
    void defaultTtl_is5Minutes() {
        QrCodeGenerator.QrToken token = generator.generate(UUID.randomUUID());
        assertEquals(300, token.ttlSeconds());
    }

    @Test
    @DisplayName("expiry time is in the future")
    void expiryTime_isInFuture() {
        QrCodeGenerator.QrToken token = generator.generate(UUID.randomUUID());
        assertTrue(token.expiresAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("TTL clamped to minimum 300s when lower value provided")
    void ttl_clampedToMin() {
        QrCodeGenerator.QrToken token = generator.generate(UUID.randomUUID(), 60);
        assertEquals(300, token.ttlSeconds());
    }

    @Test
    @DisplayName("TTL clamped to maximum 600s when higher value provided")
    void ttl_clampedToMax() {
        QrCodeGenerator.QrToken token = generator.generate(UUID.randomUUID(), 1200);
        assertEquals(600, token.ttlSeconds());
    }

    @Test
    @DisplayName("TTL within 5-10 min bounds is accepted as-is")
    void ttl_withinBounds_accepted() {
        QrCodeGenerator.QrToken token = generator.generate(UUID.randomUUID(), 450);
        assertEquals(450, token.ttlSeconds());
    }

    @Test
    @DisplayName("different calls produce different tokens")
    void differentCalls_differentTokens() {
        UUID locationId = UUID.randomUUID();
        QrCodeGenerator.QrToken token1 = generator.generate(locationId);
        QrCodeGenerator.QrToken token2 = generator.generate(locationId);

        assertNotEquals(token1.token(), token2.token());
    }

    @Test
    @DisplayName("token is a valid SHA-256 hex string (64 chars)")
    void token_isSha256Hex() {
        QrCodeGenerator.QrToken token = generator.generate(UUID.randomUUID());
        assertEquals(64, token.token().length());
        assertTrue(token.token().matches("[0-9a-f]{64}"));
    }

    @Test
    @DisplayName("different locationIds produce different tokens")
    void differentLocations_differentTokens() {
        QrCodeGenerator.QrToken token1 = generator.generate(UUID.randomUUID());
        QrCodeGenerator.QrToken token2 = generator.generate(UUID.randomUUID());
        assertNotEquals(token1.token(), token2.token());
    }
}
