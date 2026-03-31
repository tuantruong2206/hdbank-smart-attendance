package com.hdbank.attendance.adapter.in.web.request;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CheckInRequest(
        String employeeCode,
        String employeeType,
        List<BssidSignalDto> bssidSignals,
        Double gpsLatitude,
        Double gpsLongitude,
        Double gpsAccuracy,
        String deviceId,
        Map<String, Object> deviceInfo,
        Boolean isOffline,
        UUID offlineUuid,
        Instant offlineTimestamp,
        String qrToken,
        Boolean isManualEntry,
        UUID manualLocationId,
        String manualReason
) {
    public record BssidSignalDto(String bssid, int rssi) {}
}
