package com.hdbank.attendance.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shift {
    private UUID id;
    private String name;
    private String code;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isOvernight;
    private int gracePeriodMinutes;
    private int earlyDepartureMinutes;
    private BigDecimal otMultiplier;
    private UUID organizationId;
}
