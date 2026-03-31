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
public class AnomalyEvent {
    private UUID employeeId;
    private UUID attendanceRecordId;
    private int riskScore;
    private String anomalyType;
    private String description;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
