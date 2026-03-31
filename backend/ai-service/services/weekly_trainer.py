"""Weekly model re-training scheduler using APScheduler."""

import logging
from datetime import datetime

logger = logging.getLogger(__name__)

_scheduler = None


def start_weekly_training(detector) -> None:
    """Start a background scheduler that re-trains the anomaly model weekly.

    Runs every Sunday at 02:00 AM (Vietnam time).
    """
    global _scheduler
    try:
        from apscheduler.schedulers.background import BackgroundScheduler
        from apscheduler.triggers.cron import CronTrigger

        _scheduler = BackgroundScheduler(timezone="Asia/Ho_Chi_Minh")

        def retrain_job():
            logger.info("Weekly re-training started at %s", datetime.now())
            try:
                result = detector.train_from_db()
                logger.info("Weekly re-training completed: %s", result)
            except Exception:
                logger.exception("Weekly re-training failed")

        _scheduler.add_job(
            retrain_job,
            CronTrigger(day_of_week="sun", hour=2, minute=0),
            id="weekly_retrain",
            name="Weekly anomaly model re-training",
            replace_existing=True,
        )

        # Also run initial training on startup
        _scheduler.add_job(
            retrain_job,
            "date",  # run once
            id="initial_train",
            name="Initial model training on startup",
        )

        _scheduler.start()
        logger.info("Weekly training scheduler started (every Sunday 02:00 VN time)")
    except ImportError:
        logger.warning("APScheduler not installed — weekly re-training disabled. Install with: pip install apscheduler")
    except Exception:
        logger.exception("Failed to start weekly training scheduler")


def stop_weekly_training() -> None:
    """Stop the weekly training scheduler."""
    global _scheduler
    if _scheduler:
        _scheduler.shutdown(wait=False)
        _scheduler = None
        logger.info("Weekly training scheduler stopped")
