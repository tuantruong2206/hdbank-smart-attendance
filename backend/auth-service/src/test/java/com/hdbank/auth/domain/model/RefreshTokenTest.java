package com.hdbank.auth.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    @Test
    void isExpired_true_when_past_expiry() {
        var token = RefreshToken.builder()
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        assertTrue(token.isExpired());
    }

    @Test
    void isExpired_false_when_still_valid() {
        var token = RefreshToken.builder()
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        assertFalse(token.isExpired());
    }

    @Test
    void revoke_sets_revoked_true() {
        var token = RefreshToken.builder().revoked(false).build();
        token.revoke();
        assertTrue(token.isRevoked());
    }
}
