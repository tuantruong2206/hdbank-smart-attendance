"""Tests for services.chatbot_service.ChatbotService intent detection and processing."""

import pytest


# ---------------------------------------------------------------------------
# _detect_intent()
# ---------------------------------------------------------------------------


class TestDetectIntent:
    def test_detect_intent_leave_balance(self, chatbot_service):
        assert chatbot_service._detect_intent("còn bao nhiêu ngày phép?") == "CHECK_LEAVE_BALANCE"

    def test_detect_intent_create_leave(self, chatbot_service):
        assert chatbot_service._detect_intent("xin nghỉ phép ngày mai") == "CREATE_LEAVE"

    def test_detect_intent_late_count(self, chatbot_service):
        assert chatbot_service._detect_intent("tháng này tôi đi trễ mấy lần?") == "CHECK_LATE_COUNT"

    def test_detect_intent_schedule(self, chatbot_service):
        assert chatbot_service._detect_intent("ca làm việc của tôi") == "CHECK_SCHEDULE"

    def test_detect_intent_attendance(self, chatbot_service):
        assert chatbot_service._detect_intent("hôm nay tôi chấm công chưa?") == "CHECK_ATTENDANCE"

    def test_detect_intent_late_grace(self, chatbot_service):
        assert chatbot_service._detect_intent("còn mấy lần trễ có phép?") == "CHECK_LATE_GRACE"

    def test_detect_intent_general(self, chatbot_service):
        assert chatbot_service._detect_intent("xin chào") == "GENERAL_QUERY"

    def test_detect_intent_case_insensitive(self, chatbot_service):
        """Upper-case input should still match (lowered internally)."""
        assert chatbot_service._detect_intent("NGHỈ PHÉP") == "CHECK_LEAVE_BALANCE"

    def test_detect_intent_summary(self, chatbot_service):
        assert chatbot_service._detect_intent("cho tôi xem tổng kết tháng") == "CHECK_SUMMARY"


# ---------------------------------------------------------------------------
# process_message() — return-value contract
# ---------------------------------------------------------------------------


class TestProcessMessage:
    def test_process_message_returns_required_fields(self, chatbot_service):
        """Result dict must contain message, intent, and actions."""
        from unittest.mock import patch

        with patch("services.chatbot_service.get_leave_balance", return_value=[]):
            result = chatbot_service.process_message(
                employee_id="emp-100",
                message="còn bao nhiêu ngày phép?",
            )
        assert "message" in result
        assert "intent" in result
        assert "actions" in result

    def test_process_message_create_leave_has_actions(self, chatbot_service):
        """CREATE_LEAVE intent should include SHOW_LEAVE_FORM action."""
        from unittest.mock import patch

        with patch("services.chatbot_service.create_leave_request", return_value={"id": "fake"}):
            result = chatbot_service.process_message(
                employee_id="emp-101",
                message="xin nghỉ phép ngày mai",
            )
        assert result["intent"] == "CREATE_LEAVE"
        assert "SHOW_LEAVE_FORM" in result["actions"]

    def test_process_message_check_attendance_has_action(self, chatbot_service):
        """CHECK_ATTENDANCE intent should include SHOW_TODAY_STATUS action."""
        from unittest.mock import patch

        with patch("services.chatbot_service.get_attendance_today", return_value=[]):
            result = chatbot_service.process_message(
                employee_id="emp-102",
                message="hôm nay tôi chấm công chưa?",
            )
        assert result["intent"] == "CHECK_ATTENDANCE"
        assert "SHOW_TODAY_STATUS" in result["actions"]


# ---------------------------------------------------------------------------
# _get_actions()
# ---------------------------------------------------------------------------


class TestGetActions:
    def test_actions_for_create_leave(self, chatbot_service):
        assert chatbot_service._get_actions("CREATE_LEAVE") == ["SHOW_LEAVE_FORM"]

    def test_actions_for_check_attendance(self, chatbot_service):
        assert chatbot_service._get_actions("CHECK_ATTENDANCE") == ["SHOW_TODAY_STATUS"]

    def test_actions_for_general_query(self, chatbot_service):
        assert chatbot_service._get_actions("GENERAL_QUERY") == []
