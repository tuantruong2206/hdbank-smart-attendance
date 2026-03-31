package com.hdbank.attendance.domain.service;

import com.hdbank.attendance.domain.model.ShiftConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConfigResolverTest {

    private static final UUID EMPLOYEE_ID = UUID.randomUUID();

    @Test
    @DisplayName("unit config found returns most specific config (longest path)")
    void unitConfig_returned() {
        ShiftConfig unitConfig = ShiftConfig.builder()
                .level(ShiftConfig.ConfigLevel.UNIT)
                .organizationPath("/HO/KHOI_CNTT/PHONG_DEV/BP_BACKEND")
                .gracePeriodMinutes(10)
                .build();
        ShiftConfig divisionConfig = ShiftConfig.builder()
                .level(ShiftConfig.ConfigLevel.DIVISION)
                .organizationPath("/HO/KHOI_CNTT")
                .gracePeriodMinutes(20)
                .build();

        ConfigResolver resolver = new ConfigResolver(
                id -> Optional.of("/HO/KHOI_CNTT/PHONG_DEV/BP_BACKEND"),
                path -> List.of(unitConfig, divisionConfig),
                () -> Optional.empty()
        );

        ShiftConfig result = resolver.getShiftConfig(EMPLOYEE_ID);
        assertEquals(10, result.getGracePeriodMinutes());
        assertEquals(ShiftConfig.ConfigLevel.UNIT, result.getLevel());
    }

    @Test
    @DisplayName("no unit config falls back to division config")
    void noUnitConfig_fallsToDivision() {
        ShiftConfig divisionConfig = ShiftConfig.builder()
                .level(ShiftConfig.ConfigLevel.DIVISION)
                .organizationPath("/HO/KHOI_CNTT")
                .gracePeriodMinutes(20)
                .build();

        ConfigResolver resolver = new ConfigResolver(
                id -> Optional.of("/HO/KHOI_CNTT/PHONG_DEV/BP_BACKEND"),
                path -> List.of(divisionConfig),
                () -> Optional.empty()
        );

        ShiftConfig result = resolver.getShiftConfig(EMPLOYEE_ID);
        assertEquals(20, result.getGracePeriodMinutes());
    }

    @Test
    @DisplayName("no unit or division config falls back to system default")
    void noOrgConfig_fallsToSystem() {
        ShiftConfig systemConfig = ShiftConfig.builder()
                .level(ShiftConfig.ConfigLevel.SYSTEM)
                .organizationPath("/")
                .gracePeriodMinutes(15)
                .build();

        ConfigResolver resolver = new ConfigResolver(
                id -> Optional.of("/HO/KHOI_CNTT/PHONG_DEV/BP_BACKEND"),
                path -> List.of(),
                () -> Optional.of(systemConfig)
        );

        ShiftConfig result = resolver.getShiftConfig(EMPLOYEE_ID);
        assertEquals(15, result.getGracePeriodMinutes());
        assertEquals(ShiftConfig.ConfigLevel.SYSTEM, result.getLevel());
    }

    @Test
    @DisplayName("no config at all falls back to hardcoded default")
    void noConfig_fallsToHardcoded() {
        ConfigResolver resolver = new ConfigResolver(
                id -> Optional.of("/HO/KHOI_CNTT/PHONG_DEV"),
                path -> List.of(),
                () -> Optional.empty()
        );

        ShiftConfig result = resolver.getShiftConfig(EMPLOYEE_ID);
        assertEquals(15, result.getGracePeriodMinutes());
        assertEquals(15, result.getEarlyDepartureMinutes());
        assertEquals(15, result.getRoundingIntervalMinutes());
        assertEquals(4, result.getLateGraceQuotaPerMonth());
        assertTrue(result.isOvernightShiftAllowed());
        assertEquals(ShiftConfig.ConfigLevel.SYSTEM, result.getLevel());
    }

    @Test
    @DisplayName("employee with no org path falls back to system default")
    void noOrgPath_fallsToSystem() {
        ShiftConfig systemConfig = ShiftConfig.builder()
                .level(ShiftConfig.ConfigLevel.SYSTEM)
                .gracePeriodMinutes(15)
                .build();

        ConfigResolver resolver = new ConfigResolver(
                id -> Optional.empty(),
                path -> List.of(),
                () -> Optional.of(systemConfig)
        );

        ShiftConfig result = resolver.getShiftConfig(EMPLOYEE_ID);
        assertEquals(ShiftConfig.ConfigLevel.SYSTEM, result.getLevel());
    }

    @Test
    @DisplayName("config whose path is not a prefix of employee path is excluded")
    void nonMatchingPath_excluded() {
        ShiftConfig otherUnitConfig = ShiftConfig.builder()
                .level(ShiftConfig.ConfigLevel.UNIT)
                .organizationPath("/HO/KHOI_NHANSU/PHONG_HR")
                .gracePeriodMinutes(5)
                .build();

        ConfigResolver resolver = new ConfigResolver(
                id -> Optional.of("/HO/KHOI_CNTT/PHONG_DEV/BP_BACKEND"),
                path -> List.of(otherUnitConfig),
                () -> Optional.empty()
        );

        ShiftConfig result = resolver.getShiftConfig(EMPLOYEE_ID);
        // Falls through to hardcoded since the config path doesn't match
        assertEquals(ShiftConfig.ConfigLevel.SYSTEM, result.getLevel());
    }
}
