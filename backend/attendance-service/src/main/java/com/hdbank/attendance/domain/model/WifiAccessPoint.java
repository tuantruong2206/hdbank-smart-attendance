package com.hdbank.attendance.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WifiAccessPoint {
    private UUID id;
    private UUID locationId;
    private String bssid;
    private String ssid;
    private Integer floor;
    private String signalZone;
}
