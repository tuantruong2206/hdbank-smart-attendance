#!/bin/bash

# Colors
RED='\033[0;31m'
CYAN='\033[0;36m'
GREEN='\033[0;32m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  Smart Attendance — Stopping All       ${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# Stop web dev server
echo -e "${RED}[1/3] Stopping web dashboard...${NC}"
if [ -f /tmp/sa-web-dev.pid ]; then
    WEB_PID=$(cat /tmp/sa-web-dev.pid)
    kill $WEB_PID 2>/dev/null && echo -e "${GREEN}  Stopped web dev server (PID: $WEB_PID)${NC}" || echo "  Web dev server already stopped"
    rm -f /tmp/sa-web-dev.pid
else
    # Kill any vite processes for this project
    pkill -f "vite.*smart-attendance" 2>/dev/null || true
    echo -e "${GREEN}  No web dev server PID found${NC}"
fi

# Stop backend services
echo ""
echo -e "${RED}[2/3] Stopping backend services...${NC}"
docker compose down 2>&1
echo -e "${GREEN}  Backend services stopped${NC}"

# Stop infrastructure
echo ""
echo -e "${RED}[3/3] Stopping infrastructure...${NC}"
docker compose -f docker-compose.infra.yml down 2>&1
echo -e "${GREEN}  Infrastructure stopped${NC}"

echo ""
echo -e "${CYAN}All services stopped.${NC}"
echo ""
echo -e "To also remove data volumes: ${RED}docker compose -f docker-compose.infra.yml down -v${NC}"
echo ""
