package com.hdbank.attendance.domain.valueobject;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GpsCoordinateTest {

    @Test
    void distanceTo_same_point_is_zero() {
        var p = new GpsCoordinate(10.7769, 106.7009, 10);
        assertEquals(0.0, p.distanceTo(p), 0.01);
    }

    @Test
    void distanceTo_known_points() {
        // HO (10.7769, 106.7009) to CN Q1 (10.7731, 106.7030) ~500m
        var ho = new GpsCoordinate(10.7769, 106.7009, 10);
        var cnq1 = new GpsCoordinate(10.7731, 106.7030, 10);
        double distance = ho.distanceTo(cnq1);
        assertTrue(distance > 400 && distance < 700, "Expected ~500m, got " + distance);
    }

    @Test
    void distanceTo_is_symmetric() {
        var a = new GpsCoordinate(10.7769, 106.7009, 10);
        var b = new GpsCoordinate(10.8000, 106.7200, 10);
        assertEquals(a.distanceTo(b), b.distanceTo(a), 0.01);
    }

    @Test
    void distanceTo_far_points() {
        // HCM to Hanoi ~1200km
        var hcm = new GpsCoordinate(10.8231, 106.6297, 10);
        var hanoi = new GpsCoordinate(21.0285, 105.8542, 10);
        double distance = hcm.distanceTo(hanoi);
        assertTrue(distance > 1_100_000 && distance < 1_300_000, "Expected ~1200km, got " + distance / 1000 + "km");
    }
}
