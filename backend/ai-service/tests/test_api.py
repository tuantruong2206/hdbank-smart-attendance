"""Tests for FastAPI endpoints using TestClient.

The database and Kafka dependencies are mocked so the tests run without
any external services.
"""

import sys
import os
from unittest.mock import patch, MagicMock

import pytest

# ---------------------------------------------------------------------------
# Patch heavy external dependencies BEFORE importing the app.
# This avoids SQLAlchemy trying to connect to PostgreSQL and Kafka
# trying to connect to brokers during module load.
# ---------------------------------------------------------------------------

# Patch database engine / session creation
_mock_engine = MagicMock()
_mock_session_factory = MagicMock()
_mock_session_instance = MagicMock()
_mock_session_factory.return_value = _mock_session_instance


@pytest.fixture(autouse=True)
def _patch_externals(monkeypatch):
    """Monkeypatch database and kafka before every test."""
    monkeypatch.setattr("models.database.engine", _mock_engine)
    monkeypatch.setattr("models.database.SessionLocal", _mock_session_factory)
    monkeypatch.setattr("models.database.get_db_session", lambda: _mock_session_instance)


# We need to suppress the Kafka consumer startup in the app lifespan.
# Patch it at module level so the import of `main` doesn't blow up.
with patch("services.kafka_consumer.KafkaConsumerPipeline", MagicMock()):
    from fastapi.testclient import TestClient
    from main import app

client = TestClient(app)


# ---------------------------------------------------------------------------
# Health
# ---------------------------------------------------------------------------


def test_health_endpoint():
    response = client.get("/actuator/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "UP"


# ---------------------------------------------------------------------------
# Anomaly score
# ---------------------------------------------------------------------------


def test_anomaly_score_endpoint():
    response = client.post(
        "/api/v1/ai/anomaly/score",
        json={
            "employee_id": "emp-api-001",
            "check_time": "2026-03-30T08:00:00",
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert "risk_score" in body
    assert "anomaly_type" in body
    assert "description" in body


def test_anomaly_score_with_gps():
    response = client.post(
        "/api/v1/ai/anomaly/score",
        json={
            "employee_id": "emp-api-002",
            "check_time": "2026-03-30T03:00:00",
            "gps_lat": 10.78,
            "gps_lng": 106.66,
            "device_id": "device-xyz",
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert isinstance(body["risk_score"], int)


def test_anomaly_score_missing_employee_id():
    """Missing required field should return 422."""
    response = client.post(
        "/api/v1/ai/anomaly/score",
        json={"check_time": "2026-03-30T08:00:00"},
    )
    assert response.status_code == 422


# ---------------------------------------------------------------------------
# Chatbot message
# ---------------------------------------------------------------------------


def test_chatbot_message_endpoint():
    _mock_session_instance.query.return_value.filter.return_value.first.return_value = None
    _mock_session_instance.add = MagicMock()
    _mock_session_instance.commit = MagicMock()
    _mock_session_instance.refresh = MagicMock(
        side_effect=lambda obj: setattr(obj, "id", "fake-session-id")
    )

    response = client.post(
        "/api/v1/ai/chat/message",
        json={
            "employee_id": "emp-api-010",
            "message": "xin chào",
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert "message" in body
    assert "intent" in body


def test_chatbot_message_missing_fields():
    """Missing required field should return 422."""
    response = client.post(
        "/api/v1/ai/chat/message",
        json={"message": "xin chào"},
    )
    assert response.status_code == 422
