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
public class CheckInEvent {
    private UUID employeeId;
    private String employeeCode;
    private String checkType;
    private Instant timestamp;
    private UUID organizationId;
    private UUID locationId;
    private String bssid;
    private Double gpsLat;
    private Double gpsLng;
    private String deviceId;
    private int fraudScore;
    private String status;
}
