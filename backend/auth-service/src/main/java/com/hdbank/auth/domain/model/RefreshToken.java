package com.hdbank.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    private UUID id;
    private UUID employeeId;
    private String token;
    private Instant expiresAt;
    private Instant createdAt;
    private boolean revoked;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void revoke() {
        this.revoked = true;
    }
}
