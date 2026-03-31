"""Database query helpers for chatbot and anomaly services."""

import logging
from datetime import date, datetime, time, timedelta

from sqlalchemy import and_, extract, func
from sqlalchemy.orm import Session

from models.database import (
    AttendanceRecord,
    Employee,
    EmployeeShift,
    LeaveBalance,
    LeaveRequest,
    LateGraceQuota,
    Shift,
    get_db_session,
)

logger = logging.getLogger(__name__)


def get_leave_balance(employee_id: str) -> list[dict]:
    """Query leave_balances table, return remaining days per leave type."""
    db = get_db_session()
    try:
        current_year = date.today().year
        balances = (
            db.query(LeaveBalance)
            .filter(
                LeaveBalance.employee_id == employee_id,
                LeaveBalance.year == current_year,
            )
            .all()
        )
        return [
            {
                "leave_type": b.leave_type,
                "total_days": b.total_days,
                "used_days": b.used_days,
                "remaining_days": b.remaining_days,
            }
            for b in balances
        ]
    except Exception:
        logger.exception("Error querying leave balance for %s", employee_id)
        return []
    finally:
        db.close()


def get_late_count(employee_id: str, month: int, year: int) -> int:
    """Count attendance_records where employee was late in given month/year."""
    db = get_db_session()
    try:
        count = (
            db.query(func.count(AttendanceRecord.id))
            .filter(
                AttendanceRecord.employee_id == employee_id,
                AttendanceRecord.check_type == "IN",
                AttendanceRecord.status.in_(["late", "late_unexcused"]),
                extract("month", AttendanceRecord.check_time) == month,
                extract("year", AttendanceRecord.check_time) == year,
            )
            .scalar()
        )
        return count or 0
    except Exception:
        logger.exception("Error querying late count for %s", employee_id)
        return 0
    finally:
        db.close()


def get_attendance_today(employee_id: str) -> list[dict]:
    """Query today's attendance records for the employee."""
    db = get_db_session()
    try:
        today = date.today()
        records = (
            db.query(AttendanceRecord)
            .filter(
                AttendanceRecord.employee_id == employee_id,
                func.date(AttendanceRecord.check_time) == today,
            )
            .order_by(AttendanceRecord.check_time.asc())
            .all()
        )
        return [
            {
                "check_type": r.check_type,
                "check_time": r.check_time.strftime("%H:%M") if r.check_time else None,
                "status": r.status,
                "location_id": str(r.location_id) if r.location_id else None,
            }
            for r in records
        ]
    except Exception:
        logger.exception("Error querying attendance today for %s", employee_id)
        return []
    finally:
        db.close()


def get_late_grace_quota(employee_id: str, month: int, year: int) -> dict | None:
    """Query late_grace_quota for the employee in given month/year."""
    db = get_db_session()
    try:
        quota = (
            db.query(LateGraceQuota)
            .filter(
                LateGraceQuota.employee_id == employee_id,
                LateGraceQuota.month == month,
                LateGraceQuota.year == year,
            )
            .first()
        )
        if quota:
            return {
                "max_allowed": quota.max_allowed,
                "used_count": quota.used_count,
                "remaining": quota.remaining,
            }
        return None
    except Exception:
        logger.exception("Error querying late grace quota for %s", employee_id)
        return None
    finally:
        db.close()


def get_shift_info(employee_id: str) -> dict | None:
    """Query employee_shifts + shifts tables for current shift info."""
    db = get_db_session()
    try:
        today = date.today()
        result = (
            db.query(EmployeeShift, Shift)
            .join(Shift, EmployeeShift.shift_id == Shift.id)
            .filter(
                EmployeeShift.employee_id == employee_id,
                EmployeeShift.effective_date <= today,
                (EmployeeShift.end_date.is_(None)) | (EmployeeShift.end_date >= today),
            )
            .first()
        )
        if result:
            emp_shift, shift = result
            return {
                "shift_name": shift.name,
                "start_time": shift.start_time.strftime("%H:%M") if shift.start_time else None,
                "end_time": shift.end_time.strftime("%H:%M") if shift.end_time else None,
                "grace_period_minutes": shift.grace_period_minutes,
                "is_overnight": shift.is_overnight,
            }
        return None
    except Exception:
        logger.exception("Error querying shift info for %s", employee_id)
        return None
    finally:
        db.close()


def get_attendance_summary(employee_id: str, month: int, year: int) -> dict:
    """Get attendance summary: work days, late count, absent, OT hours."""
    db = get_db_session()
    try:
        records = (
            db.query(AttendanceRecord)
            .filter(
                AttendanceRecord.employee_id == employee_id,
                AttendanceRecord.check_type == "IN",
                extract("month", AttendanceRecord.check_time) == month,
                extract("year", AttendanceRecord.check_time) == year,
            )
            .all()
        )

        work_days = len(set(r.check_time.date() for r in records if r.check_time))
        late_count = sum(1 for r in records if r.status in ("late", "late_unexcused"))
        on_time_count = sum(1 for r in records if r.status == "normal")

        # Count total business days in the month for absent calculation
        first_day = date(year, month, 1)
        if month == 12:
            last_day = date(year + 1, 1, 1) - timedelta(days=1)
        else:
            last_day = date(year, month + 1, 1) - timedelta(days=1)

        today = date.today()
        end = min(last_day, today)
        total_business_days = sum(
            1 for d in range((end - first_day).days + 1)
            if (first_day + timedelta(days=d)).weekday() < 5
        )
        absent_days = max(0, total_business_days - work_days)

        return {
            "work_days": work_days,
            "late_count": late_count,
            "on_time_count": on_time_count,
            "absent_days": absent_days,
            "total_business_days": total_business_days,
        }
    except Exception:
        logger.exception("Error querying attendance summary for %s", employee_id)
        return {
            "work_days": 0,
            "late_count": 0,
            "on_time_count": 0,
            "absent_days": 0,
            "total_business_days": 0,
        }
    finally:
        db.close()


def create_leave_request(
    employee_id: str,
    leave_type: str,
    start_date: date,
    end_date: date,
    reason: str | None = None,
) -> dict:
    """Create a leave request record in the database."""
    db = get_db_session()
    try:
        leave_req = LeaveRequest(
            employee_id=employee_id,
            leave_type=leave_type,
            start_date=start_date,
            end_date=end_date,
            reason=reason,
            status="pending",
            created_by="chatbot",
        )
        db.add(leave_req)
        db.commit()
        db.refresh(leave_req)
        return {
            "id": str(leave_req.id),
            "leave_type": leave_req.leave_type,
            "start_date": leave_req.start_date.isoformat(),
            "end_date": leave_req.end_date.isoformat(),
            "status": leave_req.status,
        }
    except Exception:
        db.rollback()
        logger.exception("Error creating leave request for %s", employee_id)
        return {}
    finally:
        db.close()


def get_historical_attendance(days: int = 90) -> list[dict]:
    """Load historical attendance data for anomaly model training.

    Returns the last `days` days of attendance records with relevant features.
    """
    db = get_db_session()
    try:
        cutoff = datetime.utcnow() - timedelta(days=days)
        records = (
            db.query(AttendanceRecord)
            .filter(AttendanceRecord.check_time >= cutoff)
            .order_by(AttendanceRecord.check_time.desc())
            .limit(50000)
            .all()
        )
        return [
            {
                "employee_id": str(r.employee_id),
                "check_time": r.check_time,
                "check_type": r.check_type,
                "gps_lat": r.gps_lat,
                "gps_lng": r.gps_lng,
                "device_id": r.device_id,
                "bssid": r.bssid,
                "location_id": str(r.location_id) if r.location_id else None,
            }
            for r in records
        ]
    except Exception:
        logger.exception("Error loading historical attendance data")
        return []
    finally:
        db.close()
