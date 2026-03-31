package com.hdbank.admin.application.dto;

import com.hdbank.admin.adapter.out.persistence.entity.WifiAccessPointJpaEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WifiSurveyRequest {
    private UUID locationId;
    private List<AccessPointEntry> accessPoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessPointEntry {
        private String bssid;
        private String ssid;
        private String floor;
        private Integer signalStrengthThreshold;
    }
}
