"""Anomaly detection API endpoints."""

from datetime import datetime

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy import func

from models.database import AnomalyScore, get_db_session
from services.anomaly_detector import AnomalyDetector

router = APIRouter()
detector = AnomalyDetector()


class ScoreRequest(BaseModel):
    employee_id: str
    check_time: str
    location_id: str | None = None
    bssid: str | None = None
    gps_lat: float | None = None
    gps_lng: float | None = None
    device_id: str | None = None


class ScoreResponse(BaseModel):
    risk_score: int
    anomaly_type: str | None
    description: str | None


class AnomalyScoreOut(BaseModel):
    id: str
    risk_score: int
    anomaly_type: str | None
    description: str | None
    check_time: str
    is_escalated: bool
    created_at: str | None


class AnomalyStatsOut(BaseModel):
    total_count: int
    avg_score: float
    by_type: dict[str, int]
    high_risk_count: int
    escalated_count: int


class TrainResponse(BaseModel):
    status: str
    record_count: int
    message: str


@router.post("/score", response_model=ScoreResponse)
async def score_anomaly(request: ScoreRequest):
    """Score a single check-in event for anomaly."""
    result = detector.score(
        employee_id=request.employee_id,
        check_time=request.check_time,
        location_id=request.location_id,
        bssid=request.bssid,
        gps_lat=request.gps_lat,
        gps_lng=request.gps_lng,
        device_id=request.device_id,
    )
    return result


@router.get("/scores/{employee_id}", response_model=list[AnomalyScoreOut])
async def get_employee_scores(
    employee_id: str,
    limit: int = Query(default=50, le=200),
    min_score: int = Query(default=0, ge=0, le=100),
):
    """Get anomaly scores for a specific employee from the database."""
    db = get_db_session()
    try:
        query = (
            db.query(AnomalyScore)
            .filter(
                AnomalyScore.employee_id == employee_id,
                AnomalyScore.risk_score >= min_score,
            )
            .order_by(AnomalyScore.created_at.desc())
            .limit(limit)
        )
        scores = query.all()
        return [
            AnomalyScoreOut(
                id=str(s.id),
                risk_score=s.risk_score,
                anomaly_type=s.anomaly_type,
                description=s.description,
                check_time=s.check_time.isoformat() if s.check_time else "",
                is_escalated=s.is_escalated or False,
                created_at=s.created_at.isoformat() if s.created_at else None,
            )
            for s in scores
        ]
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        db.close()


@router.get("/stats", response_model=AnomalyStatsOut)
async def get_anomaly_stats():
    """Get overall anomaly statistics: count by type, avg score, etc."""
    db = get_db_session()
    try:
        total_count = db.query(func.count(AnomalyScore.id)).scalar() or 0
        avg_score = db.query(func.avg(AnomalyScore.risk_score)).scalar() or 0.0
        high_risk_count = (
            db.query(func.count(AnomalyScore.id))
            .filter(AnomalyScore.risk_score >= 70)
            .scalar()
            or 0
        )
        escalated_count = (
            db.query(func.count(AnomalyScore.id))
            .filter(AnomalyScore.is_escalated == True)
            .scalar()
            or 0
        )

        # Count by anomaly_type
        type_counts = (
            db.query(AnomalyScore.anomaly_type, func.count(AnomalyScore.id))
            .filter(AnomalyScore.anomaly_type.isnot(None))
            .group_by(AnomalyScore.anomaly_type)
            .all()
        )
        by_type = {t: c for t, c in type_counts}

        return AnomalyStatsOut(
            total_count=total_count,
            avg_score=round(float(avg_score), 2),
            by_type=by_type,
            high_risk_count=high_risk_count,
            escalated_count=escalated_count,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        db.close()


@router.post("/train", response_model=TrainResponse)
async def train_model():
    """Trigger model retraining from historical DB data."""
    result = detector.train_from_db()
    return TrainResponse(**result)
