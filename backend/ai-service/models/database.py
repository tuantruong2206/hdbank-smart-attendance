"""SQLAlchemy models and session management for ai-service."""

import uuid
from datetime import datetime

from sqlalchemy import (
    Boolean,
    Column,
    Date,
    DateTime,
    Enum,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
    Time,
    create_engine,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from config import settings

engine = create_engine(
    settings.DATABASE_URL,
    pool_size=10,
    max_overflow=20,
    pool_pre_ping=True,
    pool_recycle=3600,
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    pass


def get_db() -> Session:
    """Yield a database session, closing it after use."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_db_session() -> Session:
    """Return a database session (caller must close)."""
    return SessionLocal()


# ---------------------------------------------------------------------------
# AI-service owned tables
# ---------------------------------------------------------------------------


class AnomalyScore(Base):
    __tablename__ = "anomaly_scores"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    employee_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    attendance_record_id = Column(UUID(as_uuid=True), nullable=True)
    risk_score = Column(Integer, nullable=False)
    anomaly_type = Column(String(50), nullable=True)
    description = Column(Text, nullable=True)
    check_time = Column(DateTime(timezone=True), nullable=False)
    location_id = Column(UUID(as_uuid=True), nullable=True)
    device_id = Column(String(255), nullable=True)
    gps_lat = Column(Float, nullable=True)
    gps_lng = Column(Float, nullable=True)
    bssid = Column(String(50), nullable=True)
    is_escalated = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)


class ChatbotSession(Base):
    __tablename__ = "chatbot_sessions"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    employee_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    started_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    last_message_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    is_escalated = Column(Boolean, default=False)
    escalated_at = Column(DateTime(timezone=True), nullable=True)
    escalation_reason = Column(Text, nullable=True)


class ChatbotMessage(Base):
    __tablename__ = "chatbot_messages"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    session_id = Column(UUID(as_uuid=True), ForeignKey("chatbot_sessions.id"), nullable=False, index=True)
    sender = Column(String(10), nullable=False)  # 'user' or 'bot'
    message = Column(Text, nullable=False)
    intent = Column(String(50), nullable=True)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)


# ---------------------------------------------------------------------------
# Read-only models for tables owned by other services
# ---------------------------------------------------------------------------


class Employee(Base):
    __tablename__ = "employees"
    __table_args__ = {"extend_existing": True}

    id = Column(UUID(as_uuid=True), primary_key=True)
    employee_code = Column(String(20), unique=True, nullable=False)
    full_name = Column(String(100), nullable=False)
    email = Column(String(255), nullable=True)
    department_id = Column(UUID(as_uuid=True), nullable=True)
    employee_type = Column(String(20), nullable=True)  # 'nghiep_vu' or 'it_ky_thuat'
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), nullable=True)


class AttendanceRecord(Base):
    __tablename__ = "attendance_records"
    __table_args__ = {"extend_existing": True}

    id = Column(UUID(as_uuid=True), primary_key=True)
    employee_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    check_time = Column(DateTime(timezone=True), nullable=False)
    check_type = Column(String(10), nullable=True)  # 'IN' or 'OUT'
    location_id = Column(UUID(as_uuid=True), nullable=True)
    bssid = Column(String(50), nullable=True)
    gps_lat = Column(Float, nullable=True)
    gps_lng = Column(Float, nullable=True)
    device_id = Column(String(255), nullable=True)
    status = Column(String(20), nullable=True)  # 'normal', 'suspicious', 'late', etc.
    risk_score = Column(Integer, nullable=True)
    shift_id = Column(UUID(as_uuid=True), nullable=True)
    created_at = Column(DateTime(timezone=True), nullable=True)


class Shift(Base):
    __tablename__ = "shifts"
    __table_args__ = {"extend_existing": True}

    id = Column(UUID(as_uuid=True), primary_key=True)
    name = Column(String(100), nullable=False)
    start_time = Column(Time, nullable=False)
    end_time = Column(Time, nullable=False)
    grace_period_minutes = Column(Integer, default=0)
    is_overnight = Column(Boolean, default=False)


class EmployeeShift(Base):
    __tablename__ = "employee_shifts"
    __table_args__ = {"extend_existing": True}

    id = Column(UUID(as_uuid=True), primary_key=True)
    employee_id = Column(UUID(as_uuid=True), ForeignKey("employees.id"), nullable=False, index=True)
    shift_id = Column(UUID(as_uuid=True), ForeignKey("shifts.id"), nullable=False)
    effective_date = Column(Date, nullable=False)
    end_date = Column(Date, nullable=True)


class LeaveBalance(Base):
    __tablename__ = "leave_balances"
    __table_args__ = {"extend_existing": True}

    id = Column(UUID(as_uuid=True), primary_key=True)
    employee_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    leave_type = Column(String(50), nullable=False)  # 'annual', 'sick', 'personal', etc.
    year = Column(Integer, nullable=False)
    total_days = Column(Float, nullable=False)
    used_days = Column(Float, default=0)
    remaining_days = Column(Float, nullable=False)


class LeaveRequest(Base):
    __tablename__ = "leave_requests"
    __table_args__ = {"extend_existing": True}

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    employee_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    leave_type = Column(String(50), nullable=False)
    start_date = Column(Date, nullable=False)
    end_date = Column(Date, nullable=False)
    reason = Column(Text, nullable=True)
    status = Column(String(20), default="pending")  # 'pending', 'approved', 'rejected'
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    created_by = Column(String(50), nullable=True)  # 'chatbot' or 'manual'


class LateGraceQuota(Base):
    __tablename__ = "late_grace_quota"
    __table_args__ = {"extend_existing": True}

    id = Column(UUID(as_uuid=True), primary_key=True)
    employee_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    month = Column(Integer, nullable=False)
    year = Column(Integer, nullable=False)
    max_allowed = Column(Integer, default=4)
    used_count = Column(Integer, default=0)
    remaining = Column(Integer, nullable=False)
