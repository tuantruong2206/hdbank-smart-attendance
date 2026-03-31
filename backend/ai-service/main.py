"""FastAPI entry point for the AI service."""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from config import settings
from routers import anomaly, chatbot, health

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

# Kafka consumer instance (module-level so it can be shared)
_kafka_consumer = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("AI Service starting on port %d", settings.SERVICE_PORT)

    # Start Kafka consumer pipeline in background
    global _kafka_consumer
    try:
        from services.kafka_consumer import KafkaConsumerPipeline
        from services.anomaly_detector import AnomalyDetector

        detector = AnomalyDetector()
        _kafka_consumer = KafkaConsumerPipeline(detector)
        _kafka_consumer.start()
        logger.info("Kafka consumer pipeline started")

        # Start weekly re-training scheduler
        from services.weekly_trainer import start_weekly_training
        start_weekly_training(detector)

    except Exception:
        logger.exception("Failed to start Kafka consumer — running without it")

    # Load knowledge base policies
    try:
        from services.knowledge_base import load_default_policies
        loaded = load_default_policies()
        logger.info("Knowledge base: %d policy documents loaded", loaded)
    except Exception:
        logger.exception("Failed to load knowledge base — chatbot will work without RAG")

    yield

    # Shutdown
    if _kafka_consumer:
        logger.info("Stopping Kafka consumer pipeline...")
        _kafka_consumer.stop()
    try:
        from services.weekly_trainer import stop_weekly_training
        stop_weekly_training()
    except Exception:
        pass
    logger.info("AI Service shutting down")


app = FastAPI(
    title="Smart Attendance AI Service",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(anomaly.router, prefix="/api/v1/ai/anomaly", tags=["anomaly"])
app.include_router(chatbot.router, prefix="/api/v1/ai/chat", tags=["chatbot"])


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=settings.SERVICE_PORT, reload=True)
