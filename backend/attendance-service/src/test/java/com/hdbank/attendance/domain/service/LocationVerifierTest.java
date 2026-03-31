package com.hdbank.attendance.domain.service;

import com.hdbank.attendance.domain.exception.InvalidLocationException;
import com.hdbank.attendance.domain.model.WifiAccessPoint;
import com.hdbank.attendance.domain.valueobject.BssidSignal;
import com.hdbank.attendance.domain.valueobject.GpsCoordinate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LocationVerifierTest {

    private static final UUID LOCATION_ID = UUID.randomUUID();

    private WifiAccessPoint buildAp(String bssid, String ssid, int floor) {
        return WifiAccessPoint.builder()
                .id(UUID.randomUUID())
                .locationId(LOCATION_ID)
                .bssid(bssid)
                .ssid(ssid)
                .floor(floor)
                .build();
    }

    private LocationVerifier buildVerifier(Map<String, WifiAccessPoint> apMap) {
        return new LocationVerifier(
                bssid -> Optional.ofNullable(apMap.get(bssid)),
                id -> Optional.empty()
        );
    }

    @Test
    @DisplayName("WiFi BSSID match returns location with WIFI method")
    void wifiBssidMatch_returnsLocation() {
        WifiAccessPoint ap = buildAp("AA:BB:CC:DD:EE:FF", "HDBank_Floor3", 3);
        LocationVerifier verifier = buildVerifier(Map.of("AA:BB:CC:DD:EE:FF", ap));

        List<BssidSignal> signals = List.of(new BssidSignal("AA:BB:CC:DD:EE:FF", -50));
        LocationVerifier.VerificationResult result = verifier.verify(signals, null);

        assertEquals(LOCATION_ID, result.locationId());
        assertEquals("WIFI", result.method());
        assertEquals(3, result.floor());
        assertEquals("AA:BB:CC:DD:EE:FF", result.bssid());
        assertEquals("HDBank_Floor3", result.ssid());
    }

    @Test
    @DisplayName("strongest signal (highest RSSI) is selected from multiple APs")
    void strongestSignal_selected() {
        UUID locationFloor5 = UUID.randomUUID();
        WifiAccessPoint apWeak = buildAp("11:22:33:44:55:66", "HDBank_Floor2", 2);
        WifiAccessPoint apStrong = WifiAccessPoint.builder()
                .id(UUID.randomUUID())
                .locationId(locationFloor5)
                .bssid("AA:BB:CC:DD:EE:FF")
                .ssid("HDBank_Floor5")
                .floor(5)
                .build();

        Map<String, WifiAccessPoint> apMap = new HashMap<>();
        apMap.put("11:22:33:44:55:66", apWeak);
        apMap.put("AA:BB:CC:DD:EE:FF", apStrong);

        LocationVerifier verifier = buildVerifier(apMap);

        List<BssidSignal> signals = List.of(
                new BssidSignal("11:22:33:44:55:66", -80),  // weak
                new BssidSignal("AA:BB:CC:DD:EE:FF", -40)   // strong (higher RSSI)
        );

        LocationVerifier.VerificationResult result = verifier.verify(signals, null);

        assertEquals(locationFloor5, result.locationId());
        assertEquals(5, result.floor());
        assertEquals("HDBank_Floor5", result.ssid());
    }

    @Test
    @DisplayName("no WiFi match falls back to GPS verification")
    void noWifiMatch_fallsBackToGps() {
        LocationVerifier verifier = buildVerifier(Map.of());
        GpsCoordinate gps = new GpsCoordinate(10.7769, 106.7009, 5.0);

        List<BssidSignal> signals = List.of(new BssidSignal("XX:XX:XX:XX:XX:XX", -50));
        LocationVerifier.VerificationResult result = verifier.verify(signals, gps);

        assertEquals("GPS", result.method());
        assertNull(result.locationId()); // simplified GPS implementation
    }

    @Test
    @DisplayName("empty signals with GPS falls back to GPS")
    void emptySignals_fallsBackToGps() {
        LocationVerifier verifier = buildVerifier(Map.of());
        GpsCoordinate gps = new GpsCoordinate(10.7769, 106.7009, 5.0);

        LocationVerifier.VerificationResult result = verifier.verify(List.of(), gps);
        assertEquals("GPS", result.method());
    }

    @Test
    @DisplayName("null signals with GPS falls back to GPS")
    void nullSignals_fallsBackToGps() {
        LocationVerifier verifier = buildVerifier(Map.of());
        GpsCoordinate gps = new GpsCoordinate(10.7769, 106.7009, 5.0);

        LocationVerifier.VerificationResult result = verifier.verify(null, gps);
        assertEquals("GPS", result.method());
    }

    @Test
    @DisplayName("no WiFi and no GPS throws InvalidLocationException")
    void noWifiNoGps_throws() {
        LocationVerifier verifier = buildVerifier(Map.of());

        assertThrows(InvalidLocationException.class, () ->
                verifier.verify(null, null));
    }

    @Test
    @DisplayName("unrecognized BSSID with no GPS throws InvalidLocationException")
    void unrecognizedBssidNoGps_throws() {
        LocationVerifier verifier = buildVerifier(Map.of());
        List<BssidSignal> signals = List.of(new BssidSignal("XX:XX:XX:XX:XX:XX", -50));

        assertThrows(InvalidLocationException.class, () ->
                verifier.verify(signals, null));
    }
}
