package com.hdbank.attendance.domain.service;

import com.hdbank.attendance.domain.model.ShiftConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Resolves shift configuration using organization hierarchy.
 * Rule priority cascade: unit config > division config > system-wide config.
 *
 * Pure domain service — depends only on functional interfaces (ports).
 */
public class ConfigResolver {

    /** Looks up the organization path for an employee's unit. */
    private final Function<UUID, Optional<String>> employeeOrgPathLookup;

    /** Finds all ShiftConfigs whose organizationPath is a prefix of the given path. */
    private final Function<String, List<ShiftConfig>> configsByPathLookup;

    /** Looks up the system-wide default config. */
    private final java.util.function.Supplier<Optional<ShiftConfig>> systemDefaultLookup;

    public ConfigResolver(
            Function<UUID, Optional<String>> employeeOrgPathLookup,
            Function<String, List<ShiftConfig>> configsByPathLookup,
            java.util.function.Supplier<Optional<ShiftConfig>> systemDefaultLookup) {
        this.employeeOrgPathLookup = employeeOrgPathLookup;
        this.configsByPathLookup = configsByPathLookup;
        this.systemDefaultLookup = systemDefaultLookup;
    }

    /**
     * Resolve the effective ShiftConfig for an employee.
     * Traverses the org hierarchy from the employee's unit upward,
     * picking the most specific (deepest path) config available.
     *
     * @param employeeId the employee to resolve config for
     * @return the most specific ShiftConfig, or system default, or a hardcoded fallback
     */
    public ShiftConfig getShiftConfig(UUID employeeId) {
        Optional<String> orgPathOpt = employeeOrgPathLookup.apply(employeeId);

        if (orgPathOpt.isPresent()) {
            String orgPath = orgPathOpt.get();

            // Find all configs whose path is a prefix of (or equal to) the employee's org path
            List<ShiftConfig> candidates = configsByPathLookup.apply(orgPath);

            if (!candidates.isEmpty()) {
                // Sort by path length descending — longest match = most specific (unit > division)
                return candidates.stream()
                        .filter(c -> orgPath.startsWith(c.getOrganizationPath()))
                        .max(Comparator.comparingInt(c -> c.getOrganizationPath().length()))
                        .orElseGet(this::resolveSystemDefault);
            }
        }

        return resolveSystemDefault();
    }

    private ShiftConfig resolveSystemDefault() {
        return systemDefaultLookup.get().orElseGet(this::hardcodedDefault);
    }

    /**
     * Hardcoded fallback when no configuration exists in the database at all.
     */
    private ShiftConfig hardcodedDefault() {
        return ShiftConfig.builder()
                .level(ShiftConfig.ConfigLevel.SYSTEM)
                .gracePeriodMinutes(15)
                .earlyDepartureMinutes(15)
                .roundingIntervalMinutes(15)
                .lateGraceQuotaPerMonth(4)
                .overnightShiftAllowed(true)
                .build();
    }
}
