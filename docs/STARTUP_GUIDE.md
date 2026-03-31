# Smart Attendance — Startup Guide

## Prerequisites

| Tool | Version | Check Command |
|------|---------|---------------|
| Docker Desktop | 24+ | `docker --version` |
| Docker Compose | v2+ | `docker compose version` |
| Node.js | 20+ LTS | `node --version` |
| npm | 10+ | `npm --version` |

> Java JDK 21+ is only needed if running backend outside Docker (Gradle dev mode).
>
> macOS Apple Silicon: Docker Desktop must have Rosetta emulation enabled (Settings → General → Use Rosetta).

---

## Quick Start (One Command)

```bash
./scripts/start-all.sh
```

This script will:
1. Copy `.env.example` to `.env` (if not exists)
2. Start all infrastructure (PostgreSQL, Redis, Kafka, MinIO, ntfy, MailHog)
3. Wait for infrastructure to be healthy
4. Build and start all backend services via Docker
5. Install web dependencies and start Vite dev server

> First run takes ~5-10 minutes (Docker image builds). Subsequent runs are much faster.

---

## Step-by-Step Manual Startup

### Step 1: Environment Setup

```bash
cp .env.example .env    # Only first time
```

### Step 2: Start Infrastructure

```bash
docker compose -f docker-compose.infra.yml up -d
```

Verify all healthy:
```bash
docker compose -f docker-compose.infra.yml ps
```

Expected: 7-8 containers all "healthy" or "running":
- `sa-postgres-master` (port 5432)
- `sa-postgres-slave` (port 5433) — may take extra time to sync
- `sa-redis` (port 6379)
- `sa-kafka` (port 29092)
- `sa-kafka-ui` (port 8090)
- `sa-minio` (port 9000/9001)
- `sa-ntfy` (port 8095)
- `sa-mailhog` (port 1025/8025)

> **If ports 5432 or 6379 are already in use** (e.g. from another project), either stop the conflicting service or change the port in `.env` (`POSTGRES_MASTER_PORT`, `REDIS_PORT`).

### Step 3: Start Backend Services

```bash
docker compose up -d --build
```

First build takes ~5-10 minutes. Verify all 15 containers are running:
```bash
docker compose ps
```

Expected backend services:
- `sa-gateway` (port 8080)
- `sa-auth-service` (port 8081)
- `sa-attendance-service` (port 8082)
- `sa-admin-service` (port 8083)
- `sa-notification-service` (port 8084)
- `sa-report-service` (port 8085)
- `sa-ai-service` (port 8086)
- `sa-web` (port 3000) — production build via nginx

### Step 4: Start Web Dashboard (Development Mode)

For development with hot reload, also start the Vite dev server:

```bash
cd web
npm install          # Only first time
npm run dev
```

- **Development**: http://localhost:5173 (Vite dev server, hot reload, recommended for dev)
- **Production**: http://localhost:3000 (Docker nginx build, no hot reload)

> Both proxy API calls to the gateway at `localhost:8080`.

### Step 5: Start Mobile App (Expo)

```bash
cd mobile
npm install --legacy-peer-deps    # Only first time
npx expo start --port 19000
```

> **Important**: Use `--port 19000` because the default Expo port 8081 conflicts with auth-service.

Then in the Expo terminal:
- Press **`i`** → open on **iOS Simulator** (requires Xcode)
- Press **`a`** → open on **Android Emulator** (requires Android Studio + running emulator)
- Scan **QR code** → open on **physical device** via Expo Go app (no IDE needed)

#### API Connection by Platform

| Platform | API URL | How It Works |
|----------|---------|-------------|
| iOS Simulator | `localhost:8080` | Shares Mac's network |
| Android Emulator | `10.0.2.2:8080` | Special alias for host machine |
| **Physical Device (Expo Go)** | `{Mac LAN IP}:8080` | Must be on same WiFi |

The app auto-detects physical device vs emulator in `src/shared/api/axiosInstance.ts`.

#### Physical Device Setup

1. Install **Expo Go** from App Store (iPhone) or Play Store (Android)
2. Ensure phone and Mac are on the **same WiFi network**
3. Find your Mac's LAN IP:
   ```bash
   ipconfig getifaddr en0
   ```
4. The current configured IP is `10.8.48.247` in `mobile/src/shared/api/axiosInstance.ts`. If your IP is different, update it:
   ```typescript
   // In mobile/src/shared/api/axiosInstance.ts, find this line and update the IP:
   return 'http://YOUR_MAC_IP:8080/api/v1';
   ```
5. Verify gateway is reachable from the LAN IP:
   ```bash
   curl http://YOUR_MAC_IP:8080/actuator/health
   ```
6. Start Expo and scan QR code:
   ```bash
   cd mobile && npx expo start --port 19000
   ```

> **If login fails on physical device**: the most common cause is the API URL using `localhost` instead of your Mac's LAN IP. Check `axiosInstance.ts`.
>
> **If IP changes** (different WiFi network): update the IP in `axiosInstance.ts` and restart Expo with `--clear` flag.

---

## Test Accounts

| Role | Email | Password | Access |
|------|-------|----------|--------|
| System Admin | `admin@hdbank.vn` | `Admin@123` | Full access — all admin features |
| Branch Manager | `manager.q1@hdbank.vn` | `Manager@123` | Branch Q1 — approvals, team view |
| Employee (nghiệp vụ) | `nv001@hdbank.vn` | `Employee@123` | CN Q1 — check-in, leave, history |
| Employee (IT) | `it001@hdbank.vn` | `Employee@123` | HO — multi-location check-in |

> These accounts are created by Flyway migration `V999__test_data.sql` on first database startup.
> If login fails, check the Troubleshooting section below.

---

## Service URLs (Reference)

> These are the default ports. If a port doesn't work, check `.env` for overrides or run `docker compose ps` to see actual ports.

### Application

| Service | URL | Description |
|---------|-----|-------------|
| **Web Dashboard (dev)** | http://localhost:5173 | Vite dev server with hot reload (run `npm run dev` in `web/`) |
| **Web Dashboard (prod)** | http://localhost:3000 | Docker nginx production build |
| **API Gateway** | http://localhost:8080 | All API requests route through here |

### Backend Services

| Service | URL | Description |
|---------|-----|-------------|
| Auth Service | http://localhost:8081 | Login, JWT, 2FA/TOTP |
| Attendance Service | http://localhost:8082 | Check-in/out, location verify, fraud scoring |
| Admin Service | http://localhost:8083 | Org/employee CRUD, Excel import |
| Notification Service | http://localhost:8084 | Kafka consumer → push/email dispatch |
| Report Service | http://localhost:8085 | Dashboard metrics, report generation |
| AI Service | http://localhost:8086 | Anomaly detection, Vietnamese chatbot |

### Infrastructure UIs

| Service | URL | Credentials |
|---------|-----|-------------|
| **Kafka UI** | http://localhost:8090 | No auth |
| **MailHog** | http://localhost:8025 | No auth — view OTP emails here |
| **ntfy** | http://localhost:8095 | No auth — push notification server |
| **MinIO Console** | http://localhost:9001 | `minioadmin` / `minioadmin123` |

---

## API Quick Test

```bash
# 1. Login — should return accessToken and refreshToken
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@hdbank.vn","password":"Admin@123"}' | jq .

# 2. Check-in (replace TOKEN with accessToken from step 1)
curl -s -X POST http://localhost:8080/api/v1/attendance/check-in \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{
    "employeeCode":"ADMIN001",
    "employeeType":"IT_KY_THUAT",
    "bssidSignals":[{"bssid":"AA:BB:CC:DD:EE:01","rssi":-45}],
    "gpsLatitude":10.7769,
    "gpsLongitude":106.7009,
    "deviceId":"test-device"
  }' | jq .

# 3. Dashboard metrics
curl -s http://localhost:8080/api/v1/dashboard/metrics | jq .

# 4. AI Chatbot (Vietnamese)
curl -s -X POST http://localhost:8080/api/v1/ai/chat/message \
  -H "Content-Type: application/json" \
  -d '{"employee_id":"e0000000-0000-0000-0000-000000000003","message":"còn bao nhiêu ngày phép?"}' | jq .

# 5. Admin — list organizations
curl -s http://localhost:8080/api/v1/admin/organizations | jq .
```

---

## Stopping Everything

```bash
./scripts/stop-all.sh
```

Or manually:
```bash
# Stop backend + web
docker compose down

# Stop infrastructure
docker compose -f docker-compose.infra.yml down
```

Full cleanup (removes all data volumes — database, Redis, Kafka):
```bash
docker compose down -v --rmi local
docker compose -f docker-compose.infra.yml down -v
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| **Port already in use** | Run `lsof -i :PORT` to find the process. Either kill it or change the port in `.env` (e.g. `POSTGRES_MASTER_PORT=5442`) |
| **Login returns 401** | Password hashes may be stale. Re-run: `docker compose -f docker-compose.infra.yml down -v && docker compose -f docker-compose.infra.yml up -d` then `docker compose up -d --build` to re-create the database with fresh test data |
| **Gateway unhealthy / web won't start** | Gateway may need more time to start. Run `docker compose up -d` again — it will retry starting containers that depend on the gateway |
| **Kafka "Cluster ID mismatch"** | `docker compose -f docker-compose.infra.yml down -v && docker compose -f docker-compose.infra.yml up -d` |
| **PostgreSQL slave not syncing** | `docker compose -f docker-compose.infra.yml restart postgres-slave` |
| **Backend Docker build fails** | Check error with `docker compose build <service-name> 2>&1 \| tail -30`. Common cause: missing dependency in `build.gradle.kts` |
| **Web `npm install` fails** | Delete `web/node_modules` and `web/package-lock.json`, retry |
| **Mobile can't connect to API** | Android emulator: `10.0.2.2:8080`. iOS simulator: `localhost:8080`. Physical device: must use Mac's LAN IP |
| **Physical device login fails** | Most common cause: API URL uses `localhost` instead of Mac's LAN IP. Run `ipconfig getifaddr en0` to find IP, then update `mobile/src/shared/api/axiosInstance.ts`. Verify with: `curl http://YOUR_IP:8080/actuator/health` |
| **Physical device — app loads but API errors** | 1) Check phone and Mac on same WiFi. 2) Check Mac's IP hasn't changed. 3) Try: `curl http://YOUR_IP:8080/actuator/health` from another device |
| **Expo "Port 8081 in use"** | Auth-service uses 8081. Always start Expo with `--port 19000` |
| **Expo `xcrun simctl` error** | Run `xcode-select --install` or open Xcode once to accept license |
| **Expo Go can't reach API** | Phone must be on same WiFi as Mac. Update IP in `mobile/src/shared/api/axiosInstance.ts` and restart Expo with `--clear` |
| **Expo `expo-location` plugin error** | Run `cd mobile && npm install --legacy-peer-deps` to install missing native modules |
| **Android emulator can't connect** | API uses `10.0.2.2:8080` (maps to host `localhost`). Check gateway is running: `curl localhost:8080/actuator/health` |
| **"image platform mismatch" warning** | Safe to ignore on Apple Silicon — Docker runs amd64 images via Rosetta emulation |
| **Services start but API returns 502** | Backend services may still be initializing. Wait 30s and retry. Check logs: `docker compose logs <service-name> --tail 20` |
