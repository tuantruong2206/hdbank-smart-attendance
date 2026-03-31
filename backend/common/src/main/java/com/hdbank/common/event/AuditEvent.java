package com.hdbank.common.event;

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
public class AuditEvent {
    private UUID userId;
    private String userEmail;
    private String action;
    private String resource;
    private String resourceId;
    private String oldValue;
    private String newValue;
    @Builder.Default
    private Instant timestamp = Instant.now();
    private String ipAddress;
}
