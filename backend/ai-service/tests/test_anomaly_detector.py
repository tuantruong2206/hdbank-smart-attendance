"""Tests for services.anomaly_detector.AnomalyDetector."""

import pytest


# ---------------------------------------------------------------------------
# score() — return-value contract
# ---------------------------------------------------------------------------


class TestScoreContract:
    def test_score_returns_dict_with_required_keys(self, anomaly_detector):
        result = anomaly_detector.score(
            employee_id="emp-001",
            check_time="2026-03-30T08:00:00",
        )
        assert isinstance(result, dict)
        assert "risk_score" in result
        assert "anomaly_type" in result
        assert "description" in result

    def test_score_normal_checkin_low_risk(self, anomaly_detector):
        """8:00 AM weekday should be low risk (heuristic mode)."""
        result = anomaly_detector.score(
            employee_id="emp-002",
            check_time="2026-03-30T08:00:00",  # Monday 8 AM
        )
        assert result["risk_score"] < 30

    def test_score_unusual_hour_higher_risk(self, anomaly_detector):
        """3:00 AM check-in should produce a higher risk score."""
        result = anomaly_detector.score(
            employee_id="emp-003",
            check_time="2026-03-30T03:00:00",  # Monday 3 AM
        )
        # hour < 6 triggers +30, hour < 7 triggers +15 => at least 45
        assert result["risk_score"] >= 30

    def test_score_weekend_different_pattern(self, anomaly_detector):
        """Saturday check-in at normal hour — heuristic still applies."""
        result = anomaly_detector.score(
            employee_id="emp-004",
            check_time="2026-03-28T08:00:00",  # Saturday 8 AM
        )
        # The heuristic doesn't penalise weekends per se, but the score
        # should still be a valid int in [0, 100].
        assert 0 <= result["risk_score"] <= 100


# ---------------------------------------------------------------------------
# _heuristic_score()
# ---------------------------------------------------------------------------


class TestHeuristicScore:
    def test_heuristic_score_early_morning(self, anomaly_detector):
        """hour < 6 should contribute at least 30 points."""
        features = [4.0, 0, 0.0, 0, 0]  # 4 AM, Monday, no distance/device
        score = anomaly_detector._heuristic_score(features)
        assert score >= 30

    def test_heuristic_score_late_night(self, anomaly_detector):
        """hour > 22 should contribute at least 30 points."""
        features = [23.0, 2, 0.0, 0, 0]  # 11 PM, Wednesday
        score = anomaly_detector._heuristic_score(features)
        assert score >= 30

    def test_heuristic_score_normal_hour(self, anomaly_detector):
        """8:00 AM weekday with no anomalies should be low."""
        features = [8.0, 0, 0.0, 0, 0]
        score = anomaly_detector._heuristic_score(features)
        # hour 8 is between 7 and 9 inclusive, so neither extra penalty applies
        assert score < 30


# ---------------------------------------------------------------------------
# _classify_anomaly()
# ---------------------------------------------------------------------------


class TestClassifyAnomaly:
    def test_classify_anomaly_none_for_low_score(self, anomaly_detector):
        """risk_score < 30 should return None regardless of features."""
        features = [3.0, 0, 0.1, 1, 0]
        result = anomaly_detector._classify_anomaly(
            risk_score=20,
            features=features,
            employee_id="emp-010",
            device_id=None,
        )
        assert result is None

    def test_classify_anomaly_time_for_unusual_hours(self, anomaly_detector):
        """hour < 6 with risk_score >= 30 should be TIME_ANOMALY."""
        features = [3.0, 0, 0.0, 0, 0]  # 3 AM
        result = anomaly_detector._classify_anomaly(
            risk_score=50,
            features=features,
            employee_id="emp-011",
            device_id=None,
        )
        assert result == "TIME_ANOMALY"

    def test_classify_anomaly_location_for_gps_deviation(self, anomaly_detector):
        """Large distance with normal hour should be LOCATION_ANOMALY."""
        features = [9.0, 1, 0.1, 0, 0]  # 9 AM, distance > 0.05
        result = anomaly_detector._classify_anomaly(
            risk_score=50,
            features=features,
            employee_id="emp-012",
            device_id=None,
        )
        assert result == "LOCATION_ANOMALY"

    def test_classify_anomaly_device_for_new_device(self, anomaly_detector):
        """New device (device_consistency=1) should be DEVICE_ANOMALY."""
        features = [9.0, 1, 0.0, 1, 0]  # normal hour, no distance, new device
        result = anomaly_detector._classify_anomaly(
            risk_score=50,
            features=features,
            employee_id="emp-013",
            device_id="dev-new",
        )
        assert result == "DEVICE_ANOMALY"


# ---------------------------------------------------------------------------
# train()
# ---------------------------------------------------------------------------


class TestTrain:
    def test_train_does_not_crash(self, anomaly_detector):
        """train(None) should complete without error (falls back to synthetic)."""
        anomaly_detector.train(None)
        assert anomaly_detector._is_trained is True

    def test_trained_model_produces_scores(self, anomaly_detector):
        """After training, score() should use the model, not heuristic."""
        anomaly_detector.train(None)  # synthetic training
        assert anomaly_detector._is_trained is True

        result = anomaly_detector.score(
            employee_id="emp-020",
            check_time="2026-03-30T08:00:00",
        )
        assert isinstance(result["risk_score"], int)
        assert 0 <= result["risk_score"] <= 100

    def test_train_with_insufficient_data_uses_synthetic(self, anomaly_detector):
        """Fewer than 50 records should fall back to synthetic training."""
        small_data = [
            {
                "employee_id": "e1",
                "check_time": "2026-03-01T08:00:00",
                "gps_lat": 10.78,
                "gps_lng": 106.66,
                "device_id": "d1",
            }
        ] * 10  # only 10 records
        anomaly_detector.train(small_data)
        assert anomaly_detector._is_trained is True
