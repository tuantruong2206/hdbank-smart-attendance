"""Chatbot service with real DB queries and Vietnamese date parsing."""

import logging
import re
import uuid
from datetime import date, datetime, timedelta

from models.database import ChatbotMessage, ChatbotSession, get_db_session
from services.db_queries import (
    create_leave_request,
    get_attendance_summary,
    get_attendance_today,
    get_late_count,
    get_late_grace_quota,
    get_leave_balance,
    get_shift_info,
)

logger = logging.getLogger(__name__)

INTENT_PATTERNS = {
    "CHECK_LEAVE_BALANCE": [r"nghỉ phép", r"ngày phép", r"còn bao nhiêu", r"phép năm"],
    "CREATE_LEAVE": [r"xin nghỉ", r"đăng ký nghỉ", r"muốn nghỉ", r"tạo đơn nghỉ"],
    "CHECK_LATE_COUNT": [r"đi trễ", r"trễ mấy lần", r"số lần trễ", r"late"],
    "CHECK_SCHEDULE": [r"lịch làm việc", r"ca làm", r"lịch trực", r"ca nào"],
    "CHECK_ATTENDANCE": [r"chấm công", r"giờ vào", r"giờ ra", r"hôm nay"],
    "CHECK_LATE_GRACE": [r"trễ có phép", r"quota trễ", r"còn mấy lần trễ"],
    "CHECK_SUMMARY": [r"tổng kết", r"tóm tắt", r"summary", r"báo cáo tháng"],
}

# Vietnamese day-of-week mapping (thứ 2 = Monday, etc.)
_WEEKDAY_MAP = {
    "thứ 2": 0, "thứ hai": 0,
    "thứ 3": 1, "thứ ba": 1,
    "thứ 4": 2, "thứ tư": 2,
    "thứ 5": 3, "thứ năm": 3,
    "thứ 6": 4, "thứ sáu": 4,
    "thứ 7": 5, "thứ bảy": 5,
    "chủ nhật": 6, "cn": 6,
}


def _parse_vietnamese_date(text: str) -> tuple[date | None, date | None]:
    """Parse Vietnamese date expressions and return (start_date, end_date).

    Supported patterns:
      - "ngày mai"
      - "thứ 2 tới", "thứ hai tới"
      - "ngày DD/MM", "ngày DD/MM/YYYY"
      - "từ ngày DD/MM đến ngày DD/MM"
      - "từ DD/MM đến DD/MM"
    """
    text_lower = text.lower()
    today = date.today()

    # "ngày mai"
    if "ngày mai" in text_lower:
        tomorrow = today + timedelta(days=1)
        return tomorrow, tomorrow

    # "hôm nay"
    if "hôm nay" in text_lower:
        return today, today

    # "thứ X tới" / "thứ X tuần tới"
    for day_name, weekday in _WEEKDAY_MAP.items():
        if day_name in text_lower and ("tới" in text_lower or "tuần tới" in text_lower or "sau" in text_lower):
            days_ahead = weekday - today.weekday()
            if days_ahead <= 0:
                days_ahead += 7
            target = today + timedelta(days=days_ahead)
            return target, target

    # "từ ngày DD/MM đến ngày DD/MM" or "từ DD/MM đến DD/MM"
    range_match = re.search(
        r"từ\s+(?:ngày\s+)?(\d{1,2})/(\d{1,2})(?:/(\d{4}))?\s+đến\s+(?:ngày\s+)?(\d{1,2})/(\d{1,2})(?:/(\d{4}))?",
        text_lower,
    )
    if range_match:
        d1, m1 = int(range_match.group(1)), int(range_match.group(2))
        y1 = int(range_match.group(3)) if range_match.group(3) else today.year
        d2, m2 = int(range_match.group(4)), int(range_match.group(5))
        y2 = int(range_match.group(6)) if range_match.group(6) else today.year
        try:
            return date(y1, m1, d1), date(y2, m2, d2)
        except ValueError:
            pass

    # Single "ngày DD/MM" or "ngày DD/MM/YYYY"
    single_match = re.search(r"ngày\s+(\d{1,2})/(\d{1,2})(?:/(\d{4}))?", text_lower)
    if single_match:
        d, m = int(single_match.group(1)), int(single_match.group(2))
        y = int(single_match.group(3)) if single_match.group(3) else today.year
        try:
            target = date(y, m, d)
            return target, target
        except ValueError:
            pass

    return None, None


class ChatbotService:
    def process_message(
        self, employee_id: str, message: str, session_id: str | None = None
    ) -> dict:
        # Ensure session exists
        resolved_session_id = self._ensure_session(employee_id, session_id)

        intent = self._detect_intent(message)
        response = self._generate_response(intent, employee_id, message)
        actions = self._get_actions(intent)

        # Persist messages
        self._save_messages(resolved_session_id, message, response, intent)

        return {
            "message": response,
            "intent": intent,
            "actions": actions,
            "session_id": resolved_session_id,
        }

    def get_sessions(self, employee_id: str) -> list[dict]:
        """Return chatbot sessions for the employee."""
        db = get_db_session()
        try:
            sessions = (
                db.query(ChatbotSession)
                .filter(ChatbotSession.employee_id == employee_id)
                .order_by(ChatbotSession.last_message_at.desc())
                .limit(50)
                .all()
            )
            return [
                {
                    "session_id": str(s.id),
                    "started_at": s.started_at.isoformat() if s.started_at else None,
                    "last_message_at": s.last_message_at.isoformat() if s.last_message_at else None,
                    "is_escalated": s.is_escalated,
                }
                for s in sessions
            ]
        except Exception:
            logger.exception("Error fetching sessions for %s", employee_id)
            return []
        finally:
            db.close()

    def get_session_messages(self, session_id: str) -> list[dict]:
        """Return messages for a given session."""
        db = get_db_session()
        try:
            messages = (
                db.query(ChatbotMessage)
                .filter(ChatbotMessage.session_id == session_id)
                .order_by(ChatbotMessage.created_at.asc())
                .all()
            )
            return [
                {
                    "sender": m.sender,
                    "message": m.message,
                    "intent": m.intent,
                    "created_at": m.created_at.isoformat() if m.created_at else None,
                }
                for m in messages
            ]
        except Exception:
            logger.exception("Error fetching messages for session %s", session_id)
            return []
        finally:
            db.close()

    def escalate_session(self, session_id: str, reason: str | None = None) -> bool:
        """Mark a chatbot session as escalated to HR."""
        db = get_db_session()
        try:
            session = db.query(ChatbotSession).filter(ChatbotSession.id == session_id).first()
            if not session:
                return False
            session.is_escalated = True
            session.escalated_at = datetime.utcnow()
            session.escalation_reason = reason or "Chatbot không trả lời được câu hỏi"
            db.commit()
            return True
        except Exception:
            db.rollback()
            logger.exception("Error escalating session %s", session_id)
            return False
        finally:
            db.close()

    def _detect_intent(self, message: str) -> str:
        message_lower = message.lower()
        for intent, patterns in INTENT_PATTERNS.items():
            for pattern in patterns:
                if re.search(pattern, message_lower):
                    return intent
        return "GENERAL_QUERY"

    def _generate_response(self, intent: str, employee_id: str, message: str) -> str:
        today = date.today()
        month = today.month
        year = today.year

        if intent == "CHECK_LEAVE_BALANCE":
            balances = get_leave_balance(employee_id)
            if not balances:
                return "Không tìm thấy thông tin ngày phép. Vui lòng liên hệ HR."
            lines = []
            for b in balances:
                lines.append(
                    f"- {b['leave_type']}: còn {b['remaining_days']}/{b['total_days']} ngày "
                    f"(đã dùng {b['used_days']} ngày)"
                )
            return f"Thông tin ngày phép năm {year}:\n" + "\n".join(lines)

        if intent == "CHECK_LATE_COUNT":
            late_count = get_late_count(employee_id, month, year)
            quota = get_late_grace_quota(employee_id, month, year)
            grace_info = ""
            if quota:
                grace_info = f" Còn {quota['remaining']}/{quota['max_allowed']} lần trễ có phép."
            return f"Tháng {month}/{year}: bạn đã đi trễ {late_count} lần.{grace_info}"

        if intent == "CHECK_ATTENDANCE":
            records = get_attendance_today(employee_id)
            if not records:
                return "Hôm nay bạn chưa có bản ghi chấm công nào."
            lines = []
            for r in records:
                lines.append(f"- {r['check_type']}: {r['check_time']} ({r['status']})")
            return "Chấm công hôm nay:\n" + "\n".join(lines)

        if intent == "CHECK_SCHEDULE":
            shift = get_shift_info(employee_id)
            if not shift:
                return "Không tìm thấy lịch ca làm việc. Vui lòng liên hệ quản lý."
            overnight = " (ca đêm)" if shift["is_overnight"] else ""
            return (
                f"Ca làm việc của bạn: {shift['shift_name']} "
                f"({shift['start_time']} - {shift['end_time']}){overnight}. "
                f"Grace period: {shift['grace_period_minutes']} phút."
            )

        if intent == "CHECK_LATE_GRACE":
            quota = get_late_grace_quota(employee_id, month, year)
            if not quota:
                return "Không tìm thấy thông tin quota trễ có phép. Mặc định: 4 lần/tháng."
            remaining = quota["remaining"]
            warning = ""
            if remaining == 1:
                warning = " ⚠ Cảnh báo: chỉ còn 1 lần!"
            elif remaining <= 0:
                warning = " 🔴 Đã hết quota trễ có phép!"
            return (
                f"Quota trễ có phép tháng {month}/{year}: "
                f"còn {remaining}/{quota['max_allowed']} lần. "
                f"Đã dùng {quota['used_count']} lần.{warning}"
            )

        if intent == "CHECK_SUMMARY":
            summary = get_attendance_summary(employee_id, month, year)
            return (
                f"Tổng kết tháng {month}/{year}:\n"
                f"- Ngày làm việc: {summary['work_days']}/{summary['total_business_days']}\n"
                f"- Đúng giờ: {summary['on_time_count']} lần\n"
                f"- Đi trễ: {summary['late_count']} lần\n"
                f"- Vắng mặt: {summary['absent_days']} ngày"
            )

        if intent == "CREATE_LEAVE":
            return self._handle_create_leave(employee_id, message)

        return (
            "Xin lỗi, tôi chưa hiểu câu hỏi của bạn. "
            "Bạn có thể hỏi về: nghỉ phép, chấm công, lịch làm việc, số lần đi trễ, "
            "quota trễ có phép, tổng kết tháng, hoặc tạo đơn xin nghỉ."
        )

    def _handle_create_leave(self, employee_id: str, message: str) -> str:
        """Parse leave request from natural language and create in DB."""
        start_date, end_date = _parse_vietnamese_date(message)

        if not start_date or not end_date:
            return (
                "Tôi sẽ giúp bạn tạo đơn xin nghỉ phép. Vui lòng cho biết ngày nghỉ.\n"
                "Ví dụ: 'Xin nghỉ ngày mai', 'Xin nghỉ từ ngày 5/4 đến ngày 7/4', "
                "'Xin nghỉ thứ 2 tới'."
            )

        # Detect leave type from message
        leave_type = "annual"
        msg_lower = message.lower()
        if "ốm" in msg_lower or "bệnh" in msg_lower:
            leave_type = "sick"
        elif "việc riêng" in msg_lower or "cá nhân" in msg_lower:
            leave_type = "personal"

        # Extract reason if present
        reason = None
        reason_match = re.search(r"(?:lý do|vì|do)\s+(.+?)(?:\.|$)", message, re.IGNORECASE)
        if reason_match:
            reason = reason_match.group(1).strip()

        result = create_leave_request(
            employee_id=employee_id,
            leave_type=leave_type,
            start_date=start_date,
            end_date=end_date,
            reason=reason,
        )

        if not result:
            return "Không thể tạo đơn nghỉ phép. Vui lòng thử lại hoặc liên hệ HR."

        days = (end_date - start_date).days + 1
        type_names = {"annual": "phép năm", "sick": "nghỉ ốm", "personal": "việc riêng"}
        type_label = type_names.get(leave_type, leave_type)

        return (
            f"Đã tạo đơn xin nghỉ {type_label} thành công!\n"
            f"- Từ: {start_date.strftime('%d/%m/%Y')}\n"
            f"- Đến: {end_date.strftime('%d/%m/%Y')}\n"
            f"- Số ngày: {days}\n"
            f"- Trạng thái: Chờ duyệt\n"
            f"Đơn sẽ được gửi đến quản lý trực tiếp để phê duyệt."
        )

    def _get_actions(self, intent: str) -> list[str]:
        if intent == "CREATE_LEAVE":
            return ["SHOW_LEAVE_FORM"]
        if intent == "CHECK_ATTENDANCE":
            return ["SHOW_TODAY_STATUS"]
        if intent == "CHECK_SUMMARY":
            return ["SHOW_MONTHLY_REPORT"]
        return []

    def _ensure_session(self, employee_id: str, session_id: str | None) -> str:
        """Get or create a chatbot session. Returns session_id string."""
        db = get_db_session()
        try:
            if session_id:
                session = db.query(ChatbotSession).filter(ChatbotSession.id == session_id).first()
                if session:
                    session.last_message_at = datetime.utcnow()
                    db.commit()
                    return str(session.id)

            # Create new session
            new_session = ChatbotSession(
                employee_id=employee_id,
            )
            db.add(new_session)
            db.commit()
            db.refresh(new_session)
            return str(new_session.id)
        except Exception:
            db.rollback()
            logger.exception("Error ensuring session")
            return str(uuid.uuid4())
        finally:
            db.close()

    def _save_messages(
        self, session_id: str, user_message: str, bot_response: str, intent: str | None
    ) -> None:
        """Persist user and bot messages to the chatbot_messages table."""
        db = get_db_session()
        try:
            user_msg = ChatbotMessage(
                session_id=session_id,
                sender="user",
                message=user_message,
                intent=intent,
            )
            bot_msg = ChatbotMessage(
                session_id=session_id,
                sender="bot",
                message=bot_response,
                intent=intent,
            )
            db.add(user_msg)
            db.add(bot_msg)
            db.commit()
        except Exception:
            db.rollback()
            logger.exception("Error saving messages to session %s", session_id)
        finally:
            db.close()
