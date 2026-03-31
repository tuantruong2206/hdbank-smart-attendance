package com.hdbank.attendance.domain.service;

import com.hdbank.attendance.domain.model.Location;
import com.hdbank.attendance.domain.valueobject.FraudScore;
import com.hdbank.attendance.domain.valueobject.GpsCoordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FraudScorer {

    public FraudScore score(Map<String, Object> deviceInfo, GpsCoordinate gps, Location expectedLocation) {
        int totalScore = 0;
        List<String> flags = new ArrayList<>();

        // Check mock location
        if (deviceInfo != null) {
            Boolean isMockLocation = (Boolean) deviceInfo.get("isMockLocation");
            if (Boolean.TRUE.equals(isMockLocation)) {
                totalScore += 40;
                flags.add("MOCK_LOCATION_DETECTED");
            }

            Boolean isVpnActive = (Boolean) deviceInfo.get("isVpnActive");
            if (Boolean.TRUE.equals(isVpnActive)) {
                totalScore += 15;
                flags.add("VPN_ACTIVE");
            }

            Boolean isRooted = (Boolean) deviceInfo.get("isRooted");
            if (Boolean.TRUE.equals(isRooted)) {
                totalScore += 20;
                flags.add("ROOTED_DEVICE");
            }
        }

        // GPS vs expected location mismatch
        if (gps != null && expectedLocation != null
                && expectedLocation.getGpsLatitude() != null
                && expectedLocation.getGpsLongitude() != null) {
            GpsCoordinate expected = new GpsCoordinate(
                    expectedLocation.getGpsLatitude(),
                    expectedLocation.getGpsLongitude(), 0);
            double distance = gps.distanceTo(expected);
            if (distance > expectedLocation.getGeofenceRadiusMeters() * 2) {
                totalScore += 30;
                flags.add("GPS_LOCATION_MISMATCH");
            } else if (distance > expectedLocation.getGeofenceRadiusMeters()) {
                totalScore += 15;
                flags.add("GPS_OUTSIDE_GEOFENCE");
            }
        }

        return new FraudScore(Math.min(totalScore, 100), flags);
    }
}
