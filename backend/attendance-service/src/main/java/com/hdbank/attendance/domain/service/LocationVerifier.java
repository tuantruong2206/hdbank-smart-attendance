package com.hdbank.attendance.domain.service;

import com.hdbank.attendance.domain.exception.InvalidLocationException;
import com.hdbank.attendance.domain.model.Location;
import com.hdbank.attendance.domain.model.WifiAccessPoint;
import com.hdbank.attendance.domain.valueobject.BssidSignal;
import com.hdbank.attendance.domain.valueobject.GpsCoordinate;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class LocationVerifier {

    private final Function<String, Optional<WifiAccessPoint>> bssidLookup;
    private final Function<java.util.UUID, Optional<Location>> locationLookup;

    public LocationVerifier(
            Function<String, Optional<WifiAccessPoint>> bssidLookup,
            Function<java.util.UUID, Optional<Location>> locationLookup) {
        this.bssidLookup = bssidLookup;
        this.locationLookup = locationLookup;
    }

    public VerificationResult verify(List<BssidSignal> signals, GpsCoordinate gps) {
        // Primary: WiFi BSSID matching
        if (signals != null && !signals.isEmpty()) {
            Optional<WifiAccessPoint> strongestMatch = signals.stream()
                    .sorted(Comparator.comparingInt(BssidSignal::rssi).reversed())
                    .map(s -> bssidLookup.apply(s.bssid()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();

            if (strongestMatch.isPresent()) {
                WifiAccessPoint ap = strongestMatch.get();
                return new VerificationResult(
                        ap.getLocationId(), "WIFI", ap.getFloor(),
                        ap.getBssid(), ap.getSsid());
            }
        }

        // Secondary: GPS geofencing
        if (gps != null) {
            // In production, query all locations and find matching geofence
            // For now, simplified distance check
            return new VerificationResult(null, "GPS", null, null, null);
        }

        throw new InvalidLocationException("Cannot verify location: no WiFi or GPS data");
    }

    public record VerificationResult(
            java.util.UUID locationId,
            String method,
            Integer floor,
            String bssid,
            String ssid
    ) {}
}
