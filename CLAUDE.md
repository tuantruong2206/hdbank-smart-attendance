# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **Chi tiết kiến trúc, tech stack, folder structure, setup guide**: xem [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)

## Project Overview

**Smart Attendance** is a smart time-tracking system for HDBank — 100 branches + Head Office, 5,000 employees. Standalone (no Core Banking/HRM/Payroll integration). Vietnamese-only UI.

Requirements document: `Smart_Attendance_FR_NFR_v7.docx` (v7.0, 30/03/2026).

## Platforms

- **Mobile App** (iOS 15+ / Android 10+): React Native (New Architecture) — check-in/out, leave requests, schedule, notifications. Native for WiFi BSSID, GPS, offline support.
- **Web Dashboard**: React + Vite + Ant Design — admin/manager dashboard (reports, approvals, config, audit).
- **Mobile App (lite)**: Manager-facing mobile for approvals on the go.

## Technology Stack (Summary)

| Layer | Technology |
|-------|-----------|
| Mobile | React Native 0.76+ (Turbo Modules), Zustand, TanStack Query, WatermelonDB |
| Web | React 18+ / Vite / Ant Design 5 / ECharts / React Hook Form + Zod |
| Backend | Java 21 + Spring Boot 3.3+, Pragmatic Hexagonal Architecture |
| Database | PostgreSQL 16 Master-Slave (PostGIS), Flyway migrations |
| Cache | Redis 7+ (L2) + Caffeine (L1 local) |
| Messaging | Apache Kafka 3.7+ (KRaft, no ZooKeeper) |
| Push | ntfy (local) → FCM (production) |
| 2FA | java-totp (TOTP) + MailHog (local) → Twilio/SES (production) |
| Storage | MinIO (local) → S3 (production) |
| Container | Docker Compose |

## Key Domain Concepts

### Organization Structure
- **Head Office**: HO → Khối → Phòng/Ban → Bộ phận. Multiple buildings/floors.
- **Branch network**: Vùng → Chi nhánh → Phòng → Bộ phận.
- Employee types: **nghiệp vụ** (fixed location) vs **IT/kỹ thuật** (multi-location).

### Check-in/Check-out
- **WiFi BSSID** (primary) + **GPS geofencing** (secondary). Floor detection via strongest RSSI.
- 5-scenario validation matrix. Anti-fraud: fake GPS, VPN, mock location, risk scoring → flag as `suspicious`.
- **Offline check-in**: cached locally, secure timestamps, UUID per record, sync within 24h.
- **Duplicate prevention**: nghiệp vụ max 1/shift; IT configurable interval (default 30 min).
- **Dynamic QR codes**: TTL 5-10 min, auto-rotate, one-time tokens.

### Shift & Attendance Rules
- Configurable per unit: grace period, rounding, overnight shifts, OT multipliers.
- Rule priority: unit > division > system-wide.
- **Late Grace Allowance**: cho phép đi trễ 4 lần/tháng (configurable per unit). "Trễ có phép" vs "trễ không phép". Quota hiển thị trên Mobile App, cảnh báo vàng khi còn 1, đỏ khi hết. HR override cho trường hợp đặc biệt. Auto-reset đầu tháng.
- Timesheet: Draft → Pending Review → Approved → Locked (snapshot at lock).

### Leave & Approval Workflow
- Multi-level approval with configurable SLA per type.
- Auto re-route when approver absent >3 days.

### Escalation
- Multi-level up org tree with configurable timeouts. Must acknowledge AND take action.

### Admin Console
- CRUD, Excel import (template → validate → dry run → Insert/Update/Upsert → rollback 24h).
- Maker-Checker for sensitive config changes.

### RBAC
- 7 roles: System Admin, CEO, Division/Region Director, Dept Head, Deputy Head, Unit Head, Employee.
- Permission matrix: action × data scope × data type.

### Homepage Dashboard per Role (Section 11.4 FR v7)
- 4 nhóm RBAC-enforced, tổng 30 widgets: Employee (11), Manager (9), Executive (7), System Admin (3).
- Platform: Web Dashboard + Mobile App (compact variants). SSE real-time updates.
- Mỗi widget mô tả chi tiết data source: API endpoint, DB table, delivery method (sync REST / SSE / Kafka).
- Data flow: attendance-service (personal stats, team pulse, approvals), report-service (aggregated KPIs, heatmap, trend), ai-service (anomaly, chatbot), admin-service (operational issues, config pending).
- Pre-aggregated tables: `report_attendance_daily`, `report_attendance_weekly`, `report_kpi_daily` (nightly batch job).
- No separate Excel dashboard file — all 30 metrics integrated in FR v7 docx Section 11.4.

### AI-Powered Features (Section 14 FR v7)
- **Anomaly Detection**: Isolation Forest + Z-score trên check-in data. Phát hiện buddy punching, bất thường vị trí, group pattern. Risk score 0-100 per record. Auto flag `suspicious` khi score ≥70, auto escalate khi ≥90. On-premise model, weekly re-training. Kafka consumer từ `attendance.checkin`.
- **AI Chatbot**: NLP tiếng Việt tích hợp Mobile App & Web App. Nhân viên hỏi tự nhiên ("còn bao nhiêu ngày phép?", "tháng này tôi đi trễ mấy lần?"). Auto-tạo đơn nghỉ phép/đổi ca từ câu nói. Knowledge base cho quy định nội bộ. RBAC-enforced (chỉ xem data của mình). Escalate đến HR khi chatbot không trả lời được.

## Architecture Overview

### Backend: Pragmatic Hexagonal (Ports & Adapters)

**Hexagonal** for complex domain services (4 services), **Layered** for simple services (3 services):

| Service | Port | Architecture | Domain Logic |
|---------|------|-------------|-------------|
| attendance-service | 8082 | **Hexagonal** | Location verify, duplicate guard, fraud scoring, shift rules, timesheet |
| auth-service | 8081 | **Hexagonal** | 2FA/TOTP flow, OTP lifecycle, device attestation |
| admin-service | 8083 | **Hexagonal** | Excel import pipeline, maker-checker, org tree rules |
| gateway | 8080 | Layered | Routing + filter only |
| ai-service | 8086 | **Hexagonal** | Anomaly detection (Isolation Forest), AI Chatbot (NLP tiếng Việt) |
| notification-service | 8084 | Layered | Kafka consumer → push/email/SMS dispatch |
| report-service | 8085 | Layered | Kafka consumer → query → generate → upload |

**Hexagonal dependency rule:**
```
domain (pure Java, zero deps) ← application (use cases + ports) ← adapter (Spring, JPA, Kafka)
```

### Frontend: Feature-Based Architecture

Both Web and Mobile use `features/ → shared/ → pages/` structure:
```
features/  → shared/       ✅ features dùng shared utils
features/  ✗ features/     ❌ features KHÔNG import lẫn nhau
shared/    ✗ features/     ❌ shared KHÔNG depend on features
```
Enforced by `eslint-plugin-boundaries`.

### Data Flow
- **Sync**: auth, check-in result, personal history, schedule view.
- **Async** (Kafka): notifications, escalation, audit logging, reports, Excel import, anomaly detection scoring.
- **Cache** (Caffeine L1 + Redis L2): BSSID lists, config, org tree. Do NOT cache check-in records.

### Kafka Topics
`attendance.checkin`, `attendance.escalation`, `attendance.notification`, `attendance.audit`, `attendance.import`, `attendance.anomaly`. Partition key: `employeeId`.

### Database
- PostgreSQL 16 + PostGIS. `attendance_records` partitioned by month.
- Master-Slave: `@Transactional(readOnly=true)` → Slave; `@Transactional` → Master.

## Quick Start

```bash
git clone https://github.com/tuantruong2206/hdbank-smart-attendance.git
cd smart-attendance && cp .env.example .env

docker compose up -d --build        # Start all
docker compose ps                   # Verify healthy

# Access:
#   Gateway:     http://localhost:8080    Kafka UI:   http://localhost:8090
#   Web:         http://localhost:3000    MailHog:    http://localhost:8025
#   ntfy:        http://localhost:8095    MinIO:      http://localhost:9001
```

> Full setup guide with prerequisites, step-by-step startup, mobile emulator setup, test accounts, and troubleshooting: see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)

## NFR Summary

- **Performance**: check-in <2s; 5,000 concurrent; dashboard <3s; p95 <500ms read / <1s write.
- **Scale**: 3x growth (300 branches, 15K employees) without code changes.
- **Uptime**: 99.5% overall, 99.9% peak hours.
- **Security**: TLS 1.2+, AES-256, JWT short expiry, device attestation, SSL pinning, NHNN compliance.
- **Data**: attendance 3yr, audit 5yr immutable, anonymize ex-employee after 1yr.

## Assumptions & Exclusions

**Assumptions**: standalone system, BYOD mobile, WiFi infrastructure ready, initial data via Excel import, standard hours 8:00-17:00 (configurable).

**Exclusions**: no Core Banking/HRM/Payroll integration (Phase 1), no face recognition, no physical terminals, no payroll calculation, Vietnamese only, no advanced BI.
