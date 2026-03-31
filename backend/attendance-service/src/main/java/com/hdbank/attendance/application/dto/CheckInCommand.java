package com.hdbank.attendance.application.dto;

import com.hdbank.attendance.domain.valueobject.BssidSignal;
import com.hdbank.attendance.domain.valueobject.GpsCoordinate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CheckInCommand(
        UUID employeeId,
        String employeeCode,
        String employeeType,
        List<BssidSignal> bssidSignals,
        GpsCoordinate gpsCoordinate,
        String deviceId,
        Map<String, Object> deviceInfo,
        boolean isOffline,
        UUID offlineUuid,
        Instant offlineTimestamp,
        String qrToken,
        boolean isManualEntry,
        UUID manualLocationId,
        String manualReason
) {}
