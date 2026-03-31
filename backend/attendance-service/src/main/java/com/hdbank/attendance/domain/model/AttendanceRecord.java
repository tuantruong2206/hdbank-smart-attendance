package com.hdbank.attendance.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {
    private UUID id;
    private UUID employeeId;
    private String employeeCode;
    private CheckType checkType;
    private Instant checkTime;
    private UUID locationId;
    private String wifiBssid;
    private String wifiSsid;
    private Integer wifiRssi;
    private Double gpsLatitude;
    private Double gpsLongitude;
    private Double gpsAccuracy;
    private String deviceId;
    private Map<String, Object> deviceInfo;
    private VerificationMethod verificationMethod;
    private Status status;
    @Builder.Default
    private int fraudScore = 0;
    @Builder.Default
    private List<String> fraudFlags = new ArrayList<>();
    private boolean isOffline;
    private UUID offlineUuid;
    private UUID shiftId;
    private String notes;
    private Instant createdAt;

    public enum CheckType { CHECK_IN, CHECK_OUT }
    public enum VerificationMethod { WIFI, GPS, QR, MANUAL }
    public enum Status { VALID, SUSPICIOUS, REJECTED, OFFLINE_SYNCED }

    public void markSuspicious(int score, List<String> flags) {
        this.fraudScore = score;
        this.fraudFlags = flags;
        if (score >= 70) {
            this.status = Status.SUSPICIOUS;
        }
    }

    public void reject(String reason) {
        this.status = Status.REJECTED;
        this.notes = reason;
    }
}
