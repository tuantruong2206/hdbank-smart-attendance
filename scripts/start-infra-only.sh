#!/bin/bash
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "Starting infrastructure only..."
[ ! -f .env ] && cp .env.example .env

docker compose -f docker-compose.infra.yml up -d

echo "Waiting for health checks..."
sleep 15
docker compose -f docker-compose.infra.yml ps

echo ""
echo "Infrastructure ready!"
echo "  PostgreSQL: localhost:5432 (user: sa_admin / pass: localdev123)"
echo "  Redis:      localhost:6379 (pass: localdev123)"
echo "  Kafka:      localhost:29092"
echo "  Kafka UI:   http://localhost:8090"
echo "  MailHog:    http://localhost:8025"
echo "  ntfy:       http://localhost:8095"
echo "  MinIO:      http://localhost:9001 (minioadmin/minioadmin123)"
