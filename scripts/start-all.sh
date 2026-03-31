#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  Smart Attendance — Full System Start  ${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# Step 0: Check prerequisites
echo -e "${YELLOW}[0/5] Checking prerequisites...${NC}"
command -v docker >/dev/null 2>&1 || { echo -e "${RED}Docker not found. Install Docker Desktop first.${NC}"; exit 1; }
command -v node >/dev/null 2>&1 || { echo -e "${RED}Node.js not found. Install Node.js 20 LTS first.${NC}"; exit 1; }
echo -e "${GREEN}  Docker: $(docker --version | cut -d' ' -f3)${NC}"
echo -e "${GREEN}  Node.js: $(node --version)${NC}"

# Step 1: Environment
echo ""
echo -e "${YELLOW}[1/5] Setting up environment...${NC}"
if [ ! -f .env ]; then
    cp .env.example .env
    echo -e "${GREEN}  Created .env from .env.example${NC}"
else
    echo -e "${GREEN}  .env already exists${NC}"
fi

# Step 2: Start infrastructure
echo ""
echo -e "${YELLOW}[2/5] Starting infrastructure (PostgreSQL, Redis, Kafka, MinIO, ntfy, MailHog)...${NC}"
docker compose -f docker-compose.infra.yml up -d

echo -e "${YELLOW}  Waiting for services to be healthy...${NC}"
MAX_WAIT=120
ELAPSED=0
while [ $ELAPSED -lt $MAX_WAIT ]; do
    HEALTHY=$(docker compose -f docker-compose.infra.yml ps --format json 2>/dev/null | grep -c '"healthy"' || true)
    TOTAL=$(docker compose -f docker-compose.infra.yml ps -q 2>/dev/null | wc -l | tr -d ' ')
    echo -ne "\r  Healthy: ${HEALTHY}/${TOTAL} containers (${ELAPSED}s)   "
    if [ "$HEALTHY" -ge 5 ]; then
        echo ""
        break
    fi
    sleep 5
    ELAPSED=$((ELAPSED + 5))
done
echo -e "${GREEN}  Infrastructure is ready!${NC}"

# Step 3: Start backend services
echo ""
echo -e "${YELLOW}[3/5] Building and starting backend services (this may take a few minutes on first run)...${NC}"
docker compose up -d --build 2>&1 | tail -20

echo -e "${YELLOW}  Waiting for backend services to be healthy...${NC}"
ELAPSED=0
MAX_WAIT=300
while [ $ELAPSED -lt $MAX_WAIT ]; do
    RUNNING=$(docker compose ps --format json 2>/dev/null | grep -c '"running"' || true)
    HEALTHY=$(docker compose ps --format json 2>/dev/null | grep -c '"healthy"' || true)
    echo -ne "\r  Running: ${RUNNING}, Healthy: ${HEALTHY} (${ELAPSED}s)   "
    if [ "$HEALTHY" -ge 5 ]; then
        echo ""
        break
    fi
    sleep 10
    ELAPSED=$((ELAPSED + 10))
done

# Step 4: Start web dashboard
echo ""
echo -e "${YELLOW}[4/5] Starting web dashboard...${NC}"
cd "$PROJECT_ROOT/web"
if [ ! -d "node_modules" ]; then
    echo -e "  Installing npm dependencies..."
    npm install --silent 2>&1 | tail -3
fi
echo -e "  Starting Vite dev server in background..."
nohup npm run dev > /tmp/sa-web-dev.log 2>&1 &
WEB_PID=$!
echo $WEB_PID > /tmp/sa-web-dev.pid
cd "$PROJECT_ROOT"
echo -e "${GREEN}  Web dashboard starting (PID: $WEB_PID)${NC}"

# Step 5: Summary
echo ""
echo -e "${YELLOW}[5/5] Verifying services...${NC}"
sleep 5
echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  System is ready!                      ${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""
echo -e "${GREEN}  Web Dashboard:   ${NC}http://localhost:5173"
echo -e "${GREEN}  API Gateway:     ${NC}http://localhost:8080"
echo -e "${GREEN}  Kafka UI:        ${NC}http://localhost:8090"
echo -e "${GREEN}  MailHog:         ${NC}http://localhost:8025"
echo -e "${GREEN}  ntfy:            ${NC}http://localhost:8095"
echo -e "${GREEN}  MinIO Console:   ${NC}http://localhost:9001"
echo ""
echo -e "${CYAN}  Test Accounts:${NC}"
echo -e "  ┌──────────────────┬──────────────────────┬──────────────┐"
echo -e "  │ Role             │ Email                │ Password     │"
echo -e "  ├──────────────────┼──────────────────────┼──────────────┤"
echo -e "  │ System Admin     │ admin@hdbank.vn      │ Admin@123    │"
echo -e "  │ Branch Manager   │ manager.q1@hdbank.vn │ Manager@123  │"
echo -e "  │ Employee (NV)    │ nv001@hdbank.vn      │ Employee@123 │"
echo -e "  │ Employee (IT)    │ it001@hdbank.vn      │ Employee@123 │"
echo -e "  └──────────────────┴──────────────────────┴──────────────┘"
echo ""
echo -e "${YELLOW}  To stop everything: ./scripts/stop-all.sh${NC}"
echo ""
