import sys
import os

# Ensure the ai-service root is on sys.path so that
# "from services.anomaly_detector import ..." works inside tests.
sys.path.insert(
    0, os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
)

import pytest


@pytest.fixture
def anomaly_detector():
    from services.anomaly_detector import AnomalyDetector

    return AnomalyDetector()


@pytest.fixture
def chatbot_service():
    """Return a ChatbotService with DB-touching methods mocked out."""
    from unittest.mock import patch, MagicMock
    import uuid

    with patch("services.chatbot_service.get_db_session") as mock_db:
        # Provide a lightweight fake session so _ensure_session / _save_messages
        # don't try to talk to a real database.
        fake_session = MagicMock()
        fake_session.id = uuid.uuid4()
        mock_db.return_value = MagicMock()
        mock_db.return_value.query.return_value.filter.return_value.first.return_value = (
            fake_session
        )

        from services.chatbot_service import ChatbotService

        svc = ChatbotService()
        # Patch instance methods that hit DB so process_message stays pure-logic.
        svc._ensure_session = lambda eid, sid: str(uuid.uuid4())
        svc._save_messages = lambda *a, **kw: None
        yield svc
