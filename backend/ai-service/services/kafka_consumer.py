"""Kafka consumer for attendance check-in events and anomaly scoring pipeline."""

import json
import logging
import threading
import uuid
from datetime import datetime

from confluent_kafka import Consumer, Producer, KafkaError

from config import settings
from models.database import AnomalyScore, get_db_session
from services.anomaly_detector import AnomalyDetector

logger = logging.getLogger(__name__)

CHECKIN_TOPIC = "attendance.checkin"
ANOMALY_TOPIC = "attendance.anomaly"


class KafkaConsumerPipeline:
    """Consumes check-in events, scores anomalies, and produces alerts."""

    def __init__(self, detector: AnomalyDetector):
        self.detector = detector
        self._running = False
        self._thread: threading.Thread | None = None

        self._consumer_conf = {
            "bootstrap.servers": settings.KAFKA_BOOTSTRAP_SERVERS,
            "group.id": "ai-service-anomaly-scorer",
            "auto.offset.reset": "latest",
            "enable.auto.commit": True,
            "session.timeout.ms": 30000,
        }
        self._producer_conf = {
            "bootstrap.servers": settings.KAFKA_BOOTSTRAP_SERVERS,
        }

    def start(self) -> None:
        """Start the consumer in a background daemon thread."""
        if self._running:
            logger.warning("Kafka consumer is already running")
            return
        self._running = True
        self._thread = threading.Thread(target=self._consume_loop, daemon=True)
        self._thread.start()
        logger.info("Kafka consumer started for topic %s", CHECKIN_TOPIC)

    def stop(self) -> None:
        """Signal the consumer to stop."""
        self._running = False
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=10)
        logger.info("Kafka consumer stopped")

    def _consume_loop(self) -> None:
        consumer = Consumer(self._consumer_conf)
        producer = Producer(self._producer_conf)

        try:
            consumer.subscribe([CHECKIN_TOPIC])
            logger.info("Subscribed to %s", CHECKIN_TOPIC)

            while self._running:
                msg = consumer.poll(timeout=1.0)
                if msg is None:
                    continue
                if msg.error():
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        continue
                    logger.error("Kafka consumer error: %s", msg.error())
                    continue

                try:
                    self._process_message(msg, producer)
                except Exception:
                    logger.exception("Error processing Kafka message")
        except Exception:
            logger.exception("Fatal error in Kafka consumer loop")
        finally:
            consumer.close()
            producer.flush(timeout=5)
            logger.info("Kafka consumer closed")

    def _process_message(self, msg, producer: Producer) -> None:
        """Score a single check-in event and handle results."""
        value = json.loads(msg.value().decode("utf-8"))

        employee_id = value.get("employee_id", "")
        check_time = value.get("check_time", "")
        location_id = value.get("location_id")
        bssid = value.get("bssid")
        gps_lat = value.get("gps_lat")
        gps_lng = value.get("gps_lng")
        device_id = value.get("device_id")
        attendance_record_id = value.get("record_id")

        # Run anomaly scoring
        result = self.detector.score(
            employee_id=employee_id,
            check_time=check_time,
            location_id=location_id,
            bssid=bssid,
            gps_lat=gps_lat,
            gps_lng=gps_lng,
            device_id=device_id,
        )

        risk_score = result["risk_score"]
        anomaly_type = result.get("anomaly_type")
        description = result.get("description")

        # Store score in DB
        self._store_score(
            employee_id=employee_id,
            attendance_record_id=attendance_record_id,
            risk_score=risk_score,
            anomaly_type=anomaly_type,
            description=description,
            check_time=check_time,
            location_id=location_id,
            device_id=device_id,
            gps_lat=gps_lat,
            gps_lng=gps_lng,
            bssid=bssid,
            is_escalated=(risk_score >= 90),
        )

        # Produce to anomaly topic if risk >= 70
        if risk_score >= 70:
            anomaly_event = {
                "employee_id": employee_id,
                "attendance_record_id": attendance_record_id,
                "risk_score": risk_score,
                "anomaly_type": anomaly_type,
                "description": description,
                "check_time": check_time,
                "scored_at": datetime.utcnow().isoformat(),
            }
            producer.produce(
                ANOMALY_TOPIC,
                key=employee_id.encode("utf-8") if employee_id else None,
                value=json.dumps(anomaly_event).encode("utf-8"),
            )
            producer.poll(0)
            logger.warning(
                "Anomaly detected: employee=%s score=%d type=%s",
                employee_id,
                risk_score,
                anomaly_type,
            )

        # Escalation alert for critical scores
        if risk_score >= 90:
            logger.critical(
                "ESCALATION: Critical anomaly for employee=%s score=%d type=%s — %s",
                employee_id,
                risk_score,
                anomaly_type,
                description,
            )

    def _store_score(
        self,
        employee_id: str,
        attendance_record_id: str | None,
        risk_score: int,
        anomaly_type: str | None,
        description: str | None,
        check_time: str,
        location_id: str | None,
        device_id: str | None,
        gps_lat: float | None,
        gps_lng: float | None,
        bssid: str | None,
        is_escalated: bool,
    ) -> None:
        """Persist anomaly score to the anomaly_scores table."""
        db = get_db_session()
        try:
            try:
                parsed_check_time = datetime.fromisoformat(check_time.replace("Z", "+00:00"))
            except (ValueError, AttributeError):
                parsed_check_time = datetime.utcnow()

            def _to_uuid(val: str | None):
                if not val:
                    return None
                try:
                    return uuid.UUID(val)
                except (ValueError, AttributeError):
                    return None

            score_record = AnomalyScore(
                employee_id=uuid.UUID(employee_id) if employee_id else uuid.uuid4(),
                attendance_record_id=_to_uuid(attendance_record_id),
                risk_score=risk_score,
                anomaly_type=anomaly_type,
                description=description,
                check_time=parsed_check_time,
                location_id=_to_uuid(location_id),
                device_id=device_id,
                gps_lat=gps_lat,
                gps_lng=gps_lng,
                bssid=bssid,
                is_escalated=is_escalated,
            )
            db.add(score_record)
            db.commit()
        except Exception:
            db.rollback()
            logger.exception("Failed to store anomaly score")
        finally:
            db.close()
