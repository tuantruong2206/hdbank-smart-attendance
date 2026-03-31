"""Anomaly detection using Isolation Forest with enhanced feature extraction."""

import logging
from collections import defaultdict
from datetime import datetime

import numpy as np
from sklearn.ensemble import IsolationForest

logger = logging.getLogger(__name__)


class AnomalyDetector:
    def __init__(self):
        self.model = IsolationForest(contamination=0.05, random_state=42)
        self._is_trained = False
        # Per-employee history for device consistency and location patterns
        self._employee_devices: dict[str, set[str]] = defaultdict(set)
        self._employee_locations: dict[str, list[tuple[float, float]]] = defaultdict(list)
        self._employee_last_checkin: dict[str, datetime] = {}

    def score(
        self,
        employee_id: str,
        check_time: str,
        location_id: str | None = None,
        bssid: str | None = None,
        gps_lat: float | None = None,
        gps_lng: float | None = None,
        device_id: str | None = None,
    ) -> dict:
        features = self._extract_features(
            employee_id, check_time, gps_lat, gps_lng, device_id
        )

        if self._is_trained:
            model_features = features[:5]  # hour, day_of_week, distance, device_consistency, time_since_last
            raw_score = self.model.decision_function([model_features])[0]
            risk_score = max(0, min(100, int((1 - raw_score) * 50)))
        else:
            risk_score = self._heuristic_score(features)

        anomaly_type = self._classify_anomaly(
            risk_score, features, employee_id, device_id
        )
        description = self._describe_anomaly(anomaly_type, risk_score)

        # Update per-employee tracking
        self._update_tracking(employee_id, check_time, gps_lat, gps_lng, device_id)

        return {
            "risk_score": risk_score,
            "anomaly_type": anomaly_type,
            "description": description,
        }

    def train(self, training_data) -> None:
        """Train from provided data list, or fall back to synthetic data."""
        logger.info("Starting model training...")

        if training_data and len(training_data) >= 50:
            self._train_from_records(training_data)
        else:
            self._train_synthetic()

        logger.info("Model training completed. is_trained=%s", self._is_trained)

    def train_from_db(self) -> dict:
        """Load historical data from DB and train the model.

        Returns a summary dict with training metrics.
        """
        from services.db_queries import get_historical_attendance

        records = get_historical_attendance(days=90)
        if len(records) < 50:
            logger.warning(
                "Insufficient historical data (%d records), using synthetic fallback",
                len(records),
            )
            self._train_synthetic()
            return {
                "status": "trained_synthetic",
                "record_count": len(records),
                "message": "Not enough historical data; trained on synthetic data",
            }

        self._train_from_records(records)
        return {
            "status": "trained_from_db",
            "record_count": len(records),
            "message": f"Model trained on {len(records)} historical records",
        }

    def _train_from_records(self, records: list[dict]) -> None:
        """Extract features from historical records and fit the model."""
        feature_rows = []
        # Build per-employee history for distance and device consistency
        emp_locations: dict[str, list[tuple[float, float]]] = defaultdict(list)
        emp_devices: dict[str, set[str]] = defaultdict(set)
        emp_last: dict[str, datetime] = {}

        for r in records:
            eid = r.get("employee_id", "")
            ct = r.get("check_time")
            lat = r.get("gps_lat")
            lng = r.get("gps_lng")
            dev = r.get("device_id")

            if isinstance(ct, str):
                try:
                    ct = datetime.fromisoformat(ct.replace("Z", "+00:00"))
                except (ValueError, AttributeError):
                    continue
            if ct is None:
                continue

            hour = ct.hour + ct.minute / 60.0
            day_of_week = ct.weekday()

            # Distance from usual location
            if lat and lng and emp_locations[eid]:
                avg_lat = np.mean([loc[0] for loc in emp_locations[eid][-30:]])
                avg_lng = np.mean([loc[1] for loc in emp_locations[eid][-30:]])
                distance = np.sqrt((lat - avg_lat) ** 2 + (lng - avg_lng) ** 2)
            else:
                distance = 0.0

            # Device consistency (0 = known, 1 = new)
            device_consistency = 0.0
            if dev:
                device_consistency = 0.0 if dev in emp_devices[eid] else 1.0
                emp_devices[eid].add(dev)

            # Time since last check-in (hours)
            time_since = 0.0
            if eid in emp_last:
                delta = ct - emp_last[eid]
                time_since = max(0, delta.total_seconds() / 3600.0)
            emp_last[eid] = ct

            if lat and lng:
                emp_locations[eid].append((lat, lng))

            feature_rows.append([hour, day_of_week, distance, device_consistency, time_since])

        if len(feature_rows) >= 50:
            X = np.array(feature_rows)
            self.model.fit(X)
            self._is_trained = True
            # Store history for runtime scoring
            self._employee_devices = emp_devices
            self._employee_locations = emp_locations
            self._employee_last_checkin = emp_last
        else:
            self._train_synthetic()

    def _train_synthetic(self) -> None:
        """Generate synthetic training data for initial model."""
        np.random.seed(42)
        n_samples = 1000
        X = np.column_stack([
            np.random.normal(8, 1, n_samples),        # hour_of_day
            np.random.randint(0, 5, n_samples),        # day_of_week
            np.abs(np.random.normal(0, 0.005, n_samples)),  # distance
            np.random.choice([0, 0, 0, 1], n_samples),     # device_consistency
            np.random.exponential(12, n_samples),           # time_since_last
        ])
        self.model.fit(X)
        self._is_trained = True

    def _extract_features(
        self,
        employee_id: str,
        check_time: str,
        gps_lat: float | None,
        gps_lng: float | None,
        device_id: str | None,
    ) -> list:
        """Extract enhanced features: hour, day_of_week, distance, device_consistency, time_since_last."""
        try:
            dt = datetime.fromisoformat(check_time.replace("Z", "+00:00"))
            hour = dt.hour + dt.minute / 60.0
            day_of_week = dt.weekday()
        except (ValueError, AttributeError):
            dt = datetime.utcnow()
            hour = 8.0
            day_of_week = 0

        # Distance from usual location
        distance = 0.0
        if gps_lat and gps_lng and self._employee_locations.get(employee_id):
            locs = self._employee_locations[employee_id][-30:]
            avg_lat = np.mean([loc[0] for loc in locs])
            avg_lng = np.mean([loc[1] for loc in locs])
            distance = np.sqrt((gps_lat - avg_lat) ** 2 + (gps_lng - avg_lng) ** 2)

        # Device consistency
        device_consistency = 0.0
        if device_id and self._employee_devices.get(employee_id):
            device_consistency = 0.0 if device_id in self._employee_devices[employee_id] else 1.0

        # Time since last check-in
        time_since = 0.0
        if employee_id in self._employee_last_checkin:
            last = self._employee_last_checkin[employee_id]
            if dt.tzinfo and last.tzinfo is None:
                from datetime import timezone
                last = last.replace(tzinfo=timezone.utc)
            elif last.tzinfo and dt.tzinfo is None:
                from datetime import timezone
                dt = dt.replace(tzinfo=timezone.utc)
            delta = dt - last
            time_since = max(0, delta.total_seconds() / 3600.0)

        return [hour, day_of_week, distance, device_consistency, time_since]

    def _update_tracking(
        self,
        employee_id: str,
        check_time: str,
        gps_lat: float | None,
        gps_lng: float | None,
        device_id: str | None,
    ) -> None:
        """Update per-employee location, device, and last check-in tracking."""
        try:
            dt = datetime.fromisoformat(check_time.replace("Z", "+00:00"))
        except (ValueError, AttributeError):
            dt = datetime.utcnow()

        self._employee_last_checkin[employee_id] = dt
        if gps_lat and gps_lng:
            self._employee_locations[employee_id].append((gps_lat, gps_lng))
            # Keep only last 100 locations per employee
            if len(self._employee_locations[employee_id]) > 100:
                self._employee_locations[employee_id] = self._employee_locations[employee_id][-100:]
        if device_id:
            self._employee_devices[employee_id].add(device_id)

    def _heuristic_score(self, features: list) -> int:
        hour = features[0]
        distance = features[2] if len(features) > 2 else 0
        device_new = features[3] if len(features) > 3 else 0

        score = 0
        # Time-based heuristics
        if hour < 6 or hour > 22:
            score += 30
        if hour > 9 or hour < 7:
            score += 15
        # Distance heuristic
        if distance > 0.05:
            score += 25
        # New device heuristic
        if device_new > 0:
            score += 15
        return min(score, 100)

    def _classify_anomaly(
        self,
        risk_score: int,
        features: list,
        employee_id: str,
        device_id: str | None,
    ) -> str | None:
        if risk_score < 30:
            return None

        hour = features[0]
        distance = features[2] if len(features) > 2 else 0
        device_new = features[3] if len(features) > 3 else 0
        time_since = features[4] if len(features) > 4 else 0

        # Check for buddy punching: same device used by different employee
        if device_id and device_new == 0:
            # Device is known for this employee; check if it was recently used by others
            for eid, devices in self._employee_devices.items():
                if eid != employee_id and device_id in devices:
                    return "BUDDY_PUNCHING"

        if hour < 6 or hour > 22:
            return "TIME_ANOMALY"
        if distance > 0.05:
            return "LOCATION_ANOMALY"
        if device_new > 0:
            return "DEVICE_ANOMALY"
        if time_since < 0.1 and time_since > 0:
            return "GROUP_PATTERN"
        return "GENERAL_ANOMALY"

    def _describe_anomaly(self, anomaly_type: str | None, risk_score: int) -> str | None:
        if anomaly_type is None:
            return None
        descriptions = {
            "TIME_ANOMALY": f"Thời gian chấm công bất thường (risk: {risk_score})",
            "LOCATION_ANOMALY": f"Vị trí chấm công bất thường (risk: {risk_score})",
            "DEVICE_ANOMALY": f"Thiết bị chấm công lạ (risk: {risk_score})",
            "BUDDY_PUNCHING": f"Nghi ngờ chấm công hộ — cùng thiết bị, khác nhân viên (risk: {risk_score})",
            "GROUP_PATTERN": f"Phát hiện pattern chấm công nhóm bất thường (risk: {risk_score})",
            "GENERAL_ANOMALY": f"Phát hiện bất thường chung (risk: {risk_score})",
        }
        return descriptions.get(anomaly_type, f"Bất thường không xác định (risk: {risk_score})")
