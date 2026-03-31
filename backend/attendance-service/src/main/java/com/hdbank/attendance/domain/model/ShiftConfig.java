package com.hdbank.attendance.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Configuration for shift rules at a specific organization level.
 * Rule priority: unit > division > system-wide.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftConfig {
    private UUID id;
    private UUID organizationId;

    /** Organization hierarchy path, e.g., "/HO/KHOI_CNTT/PHONG_DEV/BO_PHAN_A" */
    private String organizationPath;

    /** Config level: SYSTEM, DIVISION, UNIT */
    private ConfigLevel level;

    private int gracePeriodMinutes;
    private int earlyDepartureMinutes;
    private int roundingIntervalMinutes;
    private int lateGraceQuotaPerMonth;
    private BigDecimal otMultiplier;

    /** Whether overnight shifts are allowed for this org unit */
    private boolean overnightShiftAllowed;

    public enum ConfigLevel {
        SYSTEM,    // system-wide default
        DIVISION,  // division/region level
        UNIT       // unit/department level (highest priority)
    }
}
