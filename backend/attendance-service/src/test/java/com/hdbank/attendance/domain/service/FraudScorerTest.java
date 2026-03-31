package com.hdbank.attendance.domain.service;

import com.hdbank.attendance.domain.model.Location;
import com.hdbank.attendance.domain.valueobject.FraudScore;
import com.hdbank.attendance.domain.valueobject.GpsCoordinate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FraudScorerTest {

    private FraudScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new FraudScorer();
    }

    private Location buildLocation(double lat, double lng, int radiusMeters) {
        return Location.builder()
                .id(UUID.randomUUID())
                .gpsLatitude(lat)
                .gpsLongitude(lng)
                .geofenceRadiusMeters(radiusMeters)
                .build();
    }

    @Test
    @DisplayName("clean device with matching GPS scores 0")
    void cleanDevice_scoresZero() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("isMockLocation", false);
        deviceInfo.put("isVpnActive", false);
        deviceInfo.put("isRooted", false);

        // GPS within geofence
        GpsCoordinate gps = new GpsCoordinate(10.7769, 106.7009, 5.0);
        Location location = buildLocation(10.7769, 106.7009, 100);

        FraudScore result = scorer.score(deviceInfo, gps, location);
        assertEquals(0, result.score());
        assertTrue(result.flags().isEmpty());
        assertFalse(result.isSuspicious());
    }

    @Test
    @DisplayName("mock location detected adds 40 points")
    void mockLocation_adds40() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("isMockLocation", true);
        deviceInfo.put("isVpnActive", false);
        deviceInfo.put("isRooted", false);

        FraudScore result = scorer.score(deviceInfo, null, null);
        assertEquals(40, result.score());
        assertTrue(result.flags().contains("MOCK_LOCATION_DETECTED"));
    }

    @Test
    @DisplayName("VPN active adds 15 points")
    void vpnActive_adds15() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("isMockLocation", false);
        deviceInfo.put("isVpnActive", true);
        deviceInfo.put("isRooted", false);

        FraudScore result = scorer.score(deviceInfo, null, null);
        assertEquals(15, result.score());
        assertTrue(result.flags().contains("VPN_ACTIVE"));
    }

    @Test
    @DisplayName("rooted device adds 20 points")
    void rootedDevice_adds20() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("isMockLocation", false);
        deviceInfo.put("isVpnActive", false);
        deviceInfo.put("isRooted", true);

        FraudScore result = scorer.score(deviceInfo, null, null);
        assertEquals(20, result.score());
        assertTrue(result.flags().contains("ROOTED_DEVICE"));
    }

    @Test
    @DisplayName("GPS mismatch beyond 2x geofence radius adds 30 points")
    void gpsMismatch_adds30() {
        Map<String, Object> deviceInfo = new HashMap<>();

        // HDBank HO at 10.7769, 106.7009 with 100m radius
        // GPS far away (approx 1km away)
        GpsCoordinate gps = new GpsCoordinate(10.786, 106.7009, 5.0);
        Location location = buildLocation(10.7769, 106.7009, 100);

        FraudScore result = scorer.score(deviceInfo, gps, location);
        assertEquals(30, result.score());
        assertTrue(result.flags().contains("GPS_LOCATION_MISMATCH"));
    }

    @Test
    @DisplayName("GPS outside geofence but within 2x radius adds 15 points")
    void gpsOutsideGeofence_adds15() {
        Map<String, Object> deviceInfo = new HashMap<>();

        // Just outside 100m but within 200m
        // ~150m away: 0.00135 degrees latitude ~ 150m
        GpsCoordinate gps = new GpsCoordinate(10.77825, 106.7009, 5.0);
        Location location = buildLocation(10.7769, 106.7009, 100);

        FraudScore result = scorer.score(deviceInfo, gps, location);
        assertEquals(15, result.score());
        assertTrue(result.flags().contains("GPS_OUTSIDE_GEOFENCE"));
    }

    @Test
    @DisplayName("multiple flags combined score correctly")
    void multipleFlags_combined() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("isMockLocation", true);  // +40
        deviceInfo.put("isVpnActive", true);      // +15
        deviceInfo.put("isRooted", true);          // +20

        FraudScore result = scorer.score(deviceInfo, null, null);
        assertEquals(75, result.score());
        assertEquals(3, result.flags().size());
        assertTrue(result.isSuspicious()); // >= 70
    }

    @Test
    @DisplayName("score capped at 100 when all flags combined exceed 100")
    void scoreCappedAt100() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("isMockLocation", true);  // +40
        deviceInfo.put("isVpnActive", true);      // +15
        deviceInfo.put("isRooted", true);          // +20

        // GPS far away: +30
        GpsCoordinate gps = new GpsCoordinate(11.0, 107.0, 5.0);
        Location location = buildLocation(10.7769, 106.7009, 100);

        FraudScore result = scorer.score(deviceInfo, gps, location);
        // 40+15+20+30 = 105, capped at 100
        assertEquals(100, result.score());
        assertTrue(result.requiresEscalation()); // >= 90
    }

    @Test
    @DisplayName("null deviceInfo does not throw")
    void nullDeviceInfo_noThrow() {
        FraudScore result = scorer.score(null, null, null);
        assertEquals(0, result.score());
    }

    @Test
    @DisplayName("location without GPS coordinates skips GPS check")
    void locationWithoutGps_skipsCheck() {
        Map<String, Object> deviceInfo = new HashMap<>();
        GpsCoordinate gps = new GpsCoordinate(10.7769, 106.7009, 5.0);
        Location location = Location.builder()
                .id(UUID.randomUUID())
                .gpsLatitude(null)
                .gpsLongitude(null)
                .geofenceRadiusMeters(100)
                .build();

        FraudScore result = scorer.score(deviceInfo, gps, location);
        assertEquals(0, result.score());
    }
}
