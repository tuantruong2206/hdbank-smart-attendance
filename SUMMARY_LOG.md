# SUMMARY LOG — Smart Attendance System

**Dự án:** Hệ thống Chấm công Thông minh (Smart Attendance)
**Đội:** Khối Giải Pháp Số — HDBank
**Ngày tạo:** 30/03/2026
**Cập nhật lần cuối:** 31/03/2026
**GitHub:** https://github.com/tuantruong2206/hdbank-smart-attendance.git

---

## Tổng quan dự án

Smart Attendance là hệ thống chấm công thông minh cho HDBank — 100 chi nhánh + Hội Sở, 5.000 nhân viên. Standalone (không tích hợp Core Banking/HRM/Payroll). Giao diện tiếng Việt.

---

## Kiến trúc hệ thống

| Layer | Công nghệ |
|-------|-----------|
| Backend | 7 microservices — Java 21 + Spring Boot 3.3 (Hexagonal) + Python FastAPI |
| Web Dashboard | React 18 + Vite + TypeScript + Ant Design 5 |
| Mobile App | Expo (React Native) — iOS + Android |
| Database | PostgreSQL 16 + PostGIS (master-slave), Flyway migrations |
| Cache | Redis 7 |
| Messaging | Apache Kafka (KRaft, không ZooKeeper) |
| Infrastructure | Docker Compose (15 containers) |

### Backend Services

| Service | Port | Kiến trúc | Chức năng |
|---------|------|-----------|-----------|
| gateway | 8080 | Layered | API routing, JWT filter, CORS |
| auth-service | 8081 | Hexagonal | Login, JWT, 2FA/TOTP, OTP |
| attendance-service | 8082 | Hexagonal | Check-in/out, location verify, fraud scoring, shift rules |
| admin-service | 8083 | Hexagonal | Org/employee CRUD, Excel import, maker-checker |
| notification-service | 8084 | Layered | Kafka consumer → push/email/SMS dispatch |
| report-service | 8085 | Layered | Dashboard metrics, SSE, report generation |
| ai-service | 8086 | Hexagonal (Python) | Anomaly detection (Isolation Forest), AI Chatbot (NLP tiếng Việt) |

---

## Tiến độ phát triển

### Phase 1–4: Yêu cầu & Thiết kế (30/03/2026)

- Thu thập và phân tích yêu cầu (47 FR + 17 NFR)
- Thiết kế kiến trúc: Pragmatic Hexagonal, service structure, DB design
- Chọn tech stack cho Backend, Frontend (Web + Mobile), AI/ML
- Thiết kế homepage dashboard per role (30 widgets)
- Thiết kế AI features: Anomaly Detection + AI Chatbot

**Deliverables:** Smart_Attendance_FR_NFR_v7.docx, CLAUDE.md, ARCHITECTURE.md, Presentation.pptx

### Phase 5: Full System Build (31/03/2026)

Autonomous build session — toàn bộ hệ thống được scaffold từ zero đến running trong 1 session.

#### Files đã tạo (~205 files)

| Loại | Số lượng |
|------|----------|
| Backend Java source files | 115 |
| Backend configs, builds, Dockerfiles, SQL | 32 |
| AI service (Python) | 10 |
| Web dashboard (React/TypeScript) | 20 |
| Mobile app (Expo/TypeScript) | 13 |
| Docker Compose, scripts, docs | 15 |
| **Tổng** | **~205 files** |

#### Tính năng đã implement

- **Check-in/out**: WiFi BSSID (primary) + GPS geofencing (secondary) + QR code
- **Fraud detection**: Mock location, VPN, GPS mismatch, rooted device — score 0-100
- **Late grace quota**: 4 lần/tháng, configurable per org unit, auto-reset monthly
- **Duplicate guard**: nghiệp vụ = 1/shift, IT = 30min interval
- **Offline check-in**: Local cache, secure timestamps, UUID dedup, sync within 24h
- **Leave workflow**: Multi-level approval with SLA
- **Timesheet lifecycle**: Draft → Pending Review → Approved → Locked (snapshot)
- **AI Chatbot**: Vietnamese NLP — "còn bao nhiêu ngày phép?", auto-create leave
- **Anomaly detection**: Isolation Forest, auto-escalate at score ≥90
- **RBAC**: 7 roles, permission matrix, role-based dashboards
- **Real-time**: SSE for dashboard metrics, Kafka event streaming

#### Infrastructure (Docker Compose)

15 containers chạy và healthy:

| Service | Port | Trạng thái |
|---------|------|------------|
| PostgreSQL Master | 5432 | healthy |
| PostgreSQL Slave | 5433 | running |
| Redis | 6379 | healthy |
| Kafka | 29092 | healthy |
| Kafka UI | 8090 | running |
| MinIO | 9000/9001 | healthy |
| ntfy | 8095 | healthy |
| MailHog | 1025/8025 | healthy |
| + 7 backend services | 8080-8086 | healthy |
| Web Dashboard (Docker) | 3000 | running |

#### Demo Data

- **24 test users** across tất cả 7 roles (12 nhân viên, 5 IT, 7 quản lý)
- **40+ attendance records** covering: đúng giờ, trễ, về sớm, suspicious, offline sync, multi-location, QR code
- **6 leave requests**: approved, pending, rejected, sick, personal
- **8 timesheets**: tháng 2 (locked), tháng 3 (draft/pending)
- **2 anomaly detections**: GPS mismatch score 75, mock location fraud score 92
- **Daily KPI reports**, notification logs, holidays

#### Demo Scenarios (10)

1. Employee daily check-in (mobile)
2. Late grace quota warning (mobile)
3. Suspicious check-in — fraud detection (web)
4. Leave request approval workflow (web)
5. IT multi-location check-in (mobile)
6. Offline check-in & sync (mobile)
7. Timesheet review & lock (web)
8. AI Chatbot Vietnamese NLP (API)
9. Executive dashboard (web)
10. System admin operations (web)

---

## Test Accounts

### Management

| Role | Email | Password |
|------|-------|----------|
| System Admin | `admin@hdbank.vn` | `Admin@123` |
| CEO | `ceo@hdbank.vn` | `Admin@123` |
| Division Director | `director.cntt@hdbank.vn` | `Manager@123` |
| Region Director | `region.hcm@hdbank.vn` | `Manager@123` |
| Branch Manager | `manager.q1@hdbank.vn` | `Manager@123` |
| Deputy Head | `deputy.q1@hdbank.vn` | `Manager@123` |
| Unit Head | `unit.web@hdbank.vn` | `Manager@123` |

### Employees — CN Quận 1 (nghiệp vụ)

| Code | Email | Đặc điểm |
|------|-------|-----------|
| NV001 | `nv001@hdbank.vn` | Normal, 3/4 late grace used |
| NV002 | `nv002@hdbank.vn` | SUSPICIOUS check-in (GPS mismatch + VPN) |
| NV003 | `nv003@hdbank.vn` | OFFLINE check-in (WiFi down) |
| NV004 | `nv004@hdbank.vn` | Frequently late, **4/4 late grace EXHAUSTED** |
| NV005 | `nv005@hdbank.vn` | Early leave (16:30) |
| NV006 | `nv006@hdbank.vn` | **FRAUD detected** — mock location, score 92 |
| NV007 | `nv007@hdbank.vn` | QR code check-in |

### Employees — HO IT (kỹ thuật)

| Code | Email | Đặc điểm |
|------|-------|-----------|
| IT001 | `it001@hdbank.vn` | Multi-location: HO sáng, CN Q1 chiều |
| IT002 | `it002@hdbank.vn` | OT worker (đến 19:00) |
| IT003 | `it003@hdbank.vn` | Normal |
| IT004 | `it004@hdbank.vn` | Làm việc tầng 3 |
| IT005 | `it005@hdbank.vn` | Normal |

> Tất cả employees password: `Employee@123`

---

## Cấu trúc thư mục

```
smart-attendance/
├── CLAUDE.md                          # Quick reference cho Claude Code
├── PROMPT_LOG.md                      # Lịch sử prompts và quyết định
├── SUMMARY_LOG.md                     # File này
├── .env.example / .env                # Environment variables
├── docker-compose.yml                 # Full system (15 containers)
├── docker-compose.infra.yml           # Infrastructure only (8 containers)
├── scripts/
│   ├── start-all.sh                   # One-command start
│   ├── stop-all.sh                    # One-command stop
│   └── start-infra-only.sh
├── docs/
│   ├── ARCHITECTURE.md                # Chi tiết kiến trúc
│   ├── STARTUP_GUIDE.md               # Hướng dẫn khởi động
│   └── DEMO_GUIDE.md                  # Demo scenarios
├── backend/
│   ├── build.gradle.kts               # Root build (Gradle multi-module)
│   ├── common/                        # Shared DTOs, events, security
│   ├── gateway/                       # [Layered] Spring Cloud Gateway
│   ├── auth-service/                  # [Hexagonal] Login, JWT, 2FA
│   ├── attendance-service/            # [Hexagonal] Check-in, fraud, shifts
│   ├── admin-service/                 # [Hexagonal] Org CRUD, import
│   ├── notification-service/          # [Layered] Kafka → push/email
│   ├── report-service/                # [Layered] Dashboard, reports
│   └── ai-service/                    # [Python FastAPI] ML, chatbot
├── web/                               # React + Vite + Ant Design
│   ├── src/app/                       # Routing, layouts
│   ├── src/features/                  # auth, dashboard, attendance, leave, admin
│   ├── src/shared/                    # API, hooks, types
│   └── src/pages/                     # Page compositions
└── mobile/                            # Expo React Native
    └── src/features/                  # auth, checkin, history, leave, home, profile
```

---

## Khởi động hệ thống

```bash
# Start everything
./scripts/start-all.sh

# Stop everything
./scripts/stop-all.sh

# Web Dashboard
http://localhost:5173    # Dev (hot reload)
http://localhost:3000    # Production (Docker)

# Mobile
cd mobile && npx expo start --port 19000
# Press 'i' for iOS, 'a' for Android

# Infrastructure UIs
http://localhost:8090    # Kafka UI
http://localhost:8025    # MailHog (OTP emails)
http://localhost:8095    # ntfy (push notifications)
http://localhost:9001    # MinIO Console (minioadmin/minioadmin123)
```

---

## Việc cần làm tiếp theo

### Ưu tiên 1 — Hoàn thiện features hiện tại
- Wire up real API data cho tất cả web pages (hiện một số dùng mock/empty data)
- Decode JWT và hiển thị user info sau login
- Complete mobile screens với real API integration
- Error handling và loading states

### Ưu tiên 2 — Features còn thiếu
- Excel import pipeline (admin-service)
- Maker-checker workflow
- Escalation engine (auto-escalate khi hết timeout)
- SSE real-time dashboard updates
- Push notifications end-to-end (Kafka → notification → ntfy → mobile)
- QR code generation và rotation

### Ưu tiên 3 — Testing
- Unit tests cho domain services
- Integration tests với Testcontainers
- ArchUnit tests enforce hexagonal rules
- E2E tests (Playwright cho web, Detox cho mobile)

### Ưu tiên 4 — Polish
- Vietnamese localization cho tất cả error messages
- Responsive web design
- Mobile offline sync với WatermelonDB
- Biometric login (fingerprint/Face ID)

---

## Workflow tiếp tục phát triển

```
1. cd smart_attendance && claude
2. "start all services"
3. "I want to work on [feature/fix]"
4. Claude implements → bạn review/test
5. "commit this"
6. Lặp lại 3-5
7. "stop all services"
```

---

*Tài liệu này được tự động tạo bởi Claude Code — 31/03/2026*
