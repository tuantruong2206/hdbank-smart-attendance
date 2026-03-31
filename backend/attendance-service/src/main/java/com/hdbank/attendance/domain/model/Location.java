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
public class Location {
    private UUID id;
    private UUID organizationId;
    private String name;
    private String address;
    private String building;
    private Integer floor;
    private Double gpsLatitude;
    private Double gpsLongitude;
    private int geofenceRadiusMeters;
}
