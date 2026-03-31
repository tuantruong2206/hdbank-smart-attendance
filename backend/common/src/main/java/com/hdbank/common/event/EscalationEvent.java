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
public class EscalationEvent {
    private UUID employeeId;
    private String employeeCode;
    private UUID organizationId;
    private int escalationLevel;
    private String triggerType; // ABSENT, LATE, SUSPICIOUS
    private String reason;
    private UUID escalatedTo;
    private String escalatedToCode;
    private String escalatedToRole;
    private int timeoutMinutes;
    private String status; // PENDING, ACKNOWLEDGED, ACTIONED
    @Builder.Default
    private Instant timestamp = Instant.now();
}
