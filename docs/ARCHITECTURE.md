# Smart Attendance — Architecture & Development Guide

> **Version:** 3.0 · **Date:** 30/03/2026 · **Team:** Đội Giải Pháp Số — HDBank

This document contains detailed architecture decisions, tech stack, folder structures, and development setup guide. For a quick overview, see [`CLAUDE.md`](../CLAUDE.md).

---

## 1. Technology Stack

### 1.1 Backend Tech Stack (Java Spring Boot)

| Category | Library / Tool | Version | Purpose |
|----------|---------------|---------|---------|
| Framework | Spring Boot | 3.3+ | Microservices framework |
| Java | Eclipse Temurin JDK | 21 LTS | Virtual Threads (Project Loom) cho high concurrency |
| Build | Gradle (Kotlin DSL) | 8.5+ | Multi-module monorepo, type-safe config |
| API Gateway | Spring Cloud Gateway | 4.x | Routing, rate limiting, auth filter |
| API Docs | SpringDoc OpenAPI (Swagger) | 2.5+ | Auto-generate API docs, Swagger UI test trực tiếp |
| DB Access | Spring Data JPA + Hibernate | 6.x | ORM, entity mapping, repository pattern |
| Query Builder | QueryDSL | 5.x | Type-safe dynamic queries cho reports, attendance search phức tạp |
| DB Migration | Flyway | 10+ | Version-controlled schema migrations |
| Connection Pool | HikariCP | built-in | Fastest connection pool (built-in Spring Boot) |
| Kafka | Spring Kafka | 3.2+ | KafkaTemplate, @KafkaListener |
| Cache L1 | Caffeine | 3.x | Local in-process cache cho hot data (BSSID, config) — <0.01ms |
| Cache L2 | Spring Cache + Redis | 7+ | Distributed cache — ~1ms. Two-level: Caffeine L1 → Redis L2 |
| Security | Spring Security | 6.x | JWT filter, method-level @PreAuthorize, RBAC |
| JWT | jjwt (io.jsonwebtoken) | 0.12+ | Create/verify JWT tokens, RS256 support |
| TOTP | java-totp (dev.samstevens) | 1.7+ | Google Authenticator compatible |
| Validation | Jakarta Validation + Hibernate Validator | 3.x | @Valid, custom annotations cho business rules |
| Object Mapping | MapStruct | 1.6+ | Entity ↔ DTO compile-time mapping — zero-reflection |
| Lombok | Lombok | 1.18+ | Giảm boilerplate: @Data, @Builder, @RequiredArgsConstructor |
| Excel | Apache POI + EasyExcel (Alibaba) | 5.x / 4.x | POI cho template, EasyExcel cho streaming import 10,000+ rows |
| PDF | OpenPDF | 2.x | Xuất báo cáo bảng công, attendance summary (LGPL license) |
| Scheduling | Spring Scheduler + ShedLock | — | Cron jobs + single-execution lock cho multi-replica |
| Resilience | Resilience4j | 2.x | Circuit breaker, retry, rate limiter cho inter-service calls |
| Rate Limiting | Bucket4j + Redis | 8.x | Token bucket per user/IP tại Gateway |
| Audit Trail | Javers | 7.x | Auto-track entity changes → audit_logs |
| Logging | SLF4J + Logback (JSON) | — | Structured JSON logging, MDC cho traceId/userId |
| Tracing | Micrometer Tracing + Zipkin | 1.13+ | Distributed tracing across microservices |
| Metrics | Micrometer + Prometheus | — | JVM + custom business metrics, Grafana-ready |
| Health | Spring Actuator | built-in | /actuator/health, /info, /prometheus |
| Unit Test | JUnit 5 + Mockito | 5.x | Domain + application layer testing |
| Integration Test | Testcontainers | 1.20+ | Postgres, Redis, Kafka thật trong Docker container |
| API Test | REST Assured | 5.x | Fluent HTTP API testing |
| Architecture Test | ArchUnit | 1.3+ | Enforce hexagonal dependency rules in CI |
| Static Analysis | SpotBugs + Checkstyle | — | Auto-detect bugs, enforce coding standards |

**Lý do chọn các library quan trọng:**

- **Virtual Threads (Java 21)**: Peak 7-9 AM có thể 5,000 concurrent requests. Virtual Threads cho phép mỗi request một thread mà không tốn memory. Config: `spring.threads.virtual.enabled=true`.
- **QueryDSL**: Report module cần dynamic query — filter theo branch, date range, employee type, status. QueryDSL type-safe, tránh SQL injection, IDE autocomplete.
- **Caffeine + Redis (two-level cache)**: BSSID lookup xảy ra mỗi check-in. L1 Caffeine local cache <0.01ms (TTL 5 phút), L2 Redis ~1ms (TTL 60 phút). Giảm 90% Redis roundtrips.
- **MapStruct**: Hexagonal architecture có nhiều mapping layers (domain ↔ JPA entity ↔ DTO ↔ event). MapStruct generate code lúc compile, zero runtime overhead.
- **EasyExcel (Alibaba)**: Import Excel 10,000+ rows nhân sự. Apache POI load toàn bộ vào memory gây OOM. EasyExcel streaming read từng row, ~10MB memory cho file 100MB.
- **Testcontainers**: Integration test cần Postgres thật (PostGIS, partitioning, JSONB), Redis thật, Kafka thật. Testcontainers spin Docker containers trong JUnit.
- **Resilience4j**: Circuit breaker cho inter-service calls — ngắt mạch khi downstream service chết, tránh cascade failure.
- **ShedLock**: Multi-replica production — cron jobs (tạo monthly partition, cleanup expired OTP) chỉ chạy 1 lần qua database lock.
- **ArchUnit**: Enforce hexagonal dependency rules — domain không import Spring, adapter không import domain trực tiếp. Chạy như unit test, fail CI nếu vi phạm.

### 1.2 AI/ML Tech Stack (ai-service)

| Category | Library / Tool | Version | Purpose |
|----------|---------------|---------|---------|
| ML Framework | scikit-learn (Isolation Forest) | 1.5+ | Anomaly detection model — unsupervised, on-premise |
| Stats | scipy (Z-score) | 1.x | Statistical anomaly detection bổ sung |
| NLP | underthesea | 6.x | Vietnamese NLP — tokenization, NER, POS tagging |
| LLM Gateway | LangChain | 0.2+ | Orchestrate chatbot flow, prompt template, memory |
| Vector DB | pgvector (PostgreSQL extension) | 0.7+ | Knowledge base embedding storage cho policy Q&A |
| Embedding | sentence-transformers (Vietnamese) | 2.x | Encode policy documents → vector embeddings |
| API | FastAPI | 0.110+ | Python REST API — async, auto OpenAPI docs |
| Task Queue | Celery + Redis | 5.x | Async model training, batch anomaly scoring |
| Model Registry | MLflow | 2.x | Track model versions, metrics, weekly re-training |
| Kafka | confluent-kafka-python | 2.x | Consumer từ `attendance.checkin`, producer `attendance.anomaly` |

**Lý do chọn:**

- **Isolation Forest**: Unsupervised — không cần labeled data. Phù hợp detect outlier trên check-in patterns (thời gian, vị trí, thiết bị). On-premise, không gửi data ra ngoài.
- **underthesea**: Thư viện NLP tiếng Việt mature nhất. Hỗ trợ word segmentation ("xin nghỉ phép" → "xin_nghỉ_phép"), NER, sentiment analysis.
- **FastAPI** (thay vì Spring): ML ecosystem Python-native. scikit-learn, underthesea, LangChain đều Python. FastAPI async performance tốt, auto-generate docs.
- **pgvector**: Tận dụng PostgreSQL sẵn có, không cần deploy thêm vector DB riêng. Đủ cho knowledge base ~1000 policy documents.
- **MLflow**: Track weekly re-training metrics (precision, recall, F1). So sánh model versions, auto-rollback nếu metric giảm.

### 1.3 Mobile Tech Stack (React Native)

| Category | Library | Version | Purpose |
|----------|---------|---------|---------|
| Framework | React Native (New Arch) | 0.76+ | Turbo Modules cho native WiFi/GPS, Fabric renderer |
| JS Engine | Hermes | built-in | Faster startup, lower memory footprint |
| Navigation | React Navigation | v7 | Type-safe navigation, deep linking |
| Client State | Zustand | 5.x | Lightweight state management cho UI/auth state |
| Server State | TanStack Query (React Query) | v5 | Cache, auto-retry, offline mutations, background refetch |
| Offline DB | WatermelonDB | 0.27+ | SQLite-based, lazy loading, built-in sync protocol |
| Secure Storage | react-native-keychain | 9.x | JWT/TOTP secret → iOS Keychain / Android Keystore |
| HTTP Client | Axios | 1.7+ | Interceptors cho JWT auto-refresh, request/response logging |
| WiFi Scan | react-native-wifi-reborn | 4.x | BSSID + RSSI access cho location verification |
| GPS | react-native-geolocation-service | 5.x | High accuracy, configurable timeout |
| Push Notification | react-native-notifications | 5.x | Local + remote push, badge management |
| Biometric Auth | react-native-biometrics | 3.x | Fingerprint / Face ID unlock thay password |
| SSL Pinning | react-native-ssl-pinning | 1.x | Certificate pinning — NHNN security compliance |
| Background Tasks | react-native-background-fetch | 4.x | Offline sync, auto check-out reminder |
| OTA Updates | CodePush / Expo Updates | — | Hotfix JS bundle không qua App Store review |
| Crash Reporting | @sentry/react-native | 6.x | Crash logs, performance monitoring, breadcrumbs |
| Testing | Jest + RNTL + Detox | — | Unit + Integration + E2E |
| Debug | Flipper | — | Network inspector, DB viewer, layout inspect |

**Lý do chọn các library quan trọng:**

- **Zustand + TanStack Query** (thay vì Redux): Zustand cho client state (UI, auth), TanStack Query cho server state (API data). TanStack Query giải quyết sẵn auto-retry khi mất mạng, background refetch khi app resume, cache invalidation — critical cho app chấm công offline-first.
- **WatermelonDB** (thay vì AsyncStorage): AsyncStorage là key-value plaintext, quá chậm cho structured data. WatermelonDB dùng SQLite, hỗ trợ lazy loading, query phức tạp, và có built-in sync protocol phù hợp pattern sync-within-24h.
- **react-native-keychain**: JWT token, TOTP secret, device ID **không được** lưu AsyncStorage (plaintext). Keychain/Keystore cung cấp mã hóa hardware-level, bắt buộc cho bảo mật ngân hàng.
- **react-native-ssl-pinning**: Chống man-in-the-middle attack, pin certificate của API Gateway. Yêu cầu bắt buộc theo NHNN cho ứng dụng tài chính.
- **react-native-biometrics**: Sau lần đầu login (password + 2FA), cho phép unlock bằng vân tay/Face ID. TOTP secret đã lưu trong Keychain nên có thể auto-generate OTP code.
- **react-native-background-fetch**: Cho phép sync offline check-in records khi app ở background, gửi check-out reminder, và nhận push notification khi app bị kill.
- **CodePush**: Hotfix UI/logic nhanh (rule tính công, UI bug) mà không cần chờ App Store review 2-7 ngày. Chỉ update JS bundle, không update native code.
- **Sentry**: Self-hosted option phù hợp yêu cầu data residency Vietnam. Crash reports với breadcrumbs giúp debug nhanh trên production.

### 1.4 Web Dashboard Tech Stack (React + Vite)

| Category | Library | Version | Purpose |
|----------|---------|---------|---------|
| Framework | React + Vite | 18+ / 6.x | Fast HMR, tree-shaking, TypeScript native |
| Language | TypeScript | 5.x | Type-safe — bắt buộc cho dự án banking |
| UI Components | Ant Design (antd) | 5.x | Enterprise component library: Table, Tree, Form, Steps, Transfer, Cascader |
| Routing | React Router | v7 | Nested routes, route guards cho RBAC, lazy loading |
| Client State | Zustand | 5.x | Đồng bộ pattern với Mobile |
| Server State | TanStack Query | v5 | Đồng bộ pattern với Mobile — cache, retry, polling cho real-time dashboard |
| Data Table | TanStack Table | v8 | Headless table — sorting, filtering, pagination, column resize, row selection |
| Charts | Apache ECharts (echarts-for-react) | 5.x | Line, bar, pie, heatmap, gauge — real-time update, export PNG |
| Form | React Hook Form + Zod | 7.x / 3.x | Uncontrolled performance, Zod schema reusable cho API contract |
| Date/Time | Day.js | 1.x | Lightweight (2KB), Ant Design dùng sẵn Day.js, locale Vietnamese |
| Excel Export | SheetJS (xlsx) | 0.20+ | Đọc + ghi Excel cho import/export, template generation |
| PDF Export | @react-pdf/renderer | 4.x | Xuất báo cáo bảng công, attendance report sang PDF |
| Real-time | Server-Sent Events (EventSource) | native | One-way push: dashboard metrics, notification count, approval queue |
| HTTP Client | Axios | 1.7+ | Đồng bộ pattern với Mobile — interceptors JWT refresh |
| Auth | Custom (JWT + RBAC hook) | — | `useAuth()` hook + `<PermissionGate>` component |
| Icons | Lucide React | 0.4+ | Tree-shakeable, consistent style |
| Notifications | Ant Design Notification + ntfy SSE | — | In-app toast + real-time push count |
| Testing | Vitest + Testing Library + Playwright | — | Unit + Component + E2E |
| Linting | ESLint + Prettier + Husky | — | Code quality enforcement, pre-commit hooks |
| CSS | Ant Design tokens + CSS Modules | — | Theme nhất quán qua design tokens, scoped styles khi cần custom |

**Lý do chọn các library quan trọng:**

- **Ant Design** (thay vì shadcn/ui hoặc MUI): Enterprise admin dashboard cần component phức tạp sẵn có — Tree (org structure), Transfer (permission assignment), Descriptions (detail view), Steps (approval workflow), Cascader (org hierarchy picker). Ant Design cung cấp tất cả out-of-the-box, hỗ trợ i18n Vietnamese, và được dùng rộng rãi trong banking/fintech apps tại Việt Nam.
- **ECharts** (thay vì Recharts / Chart.js): Dashboard cần heatmap (attendance pattern), gauge (real-time employee count), funnel (approval pipeline), mixed charts. ECharts mạnh hơn cho enterprise dashboards.
- **React Hook Form + Zod** (thay vì Ant Design Form built-in): Performance kém khi form phức tạp (admin config 30+ fields, conditional logic). React Hook Form uncontrolled approach nhanh hơn. Zod schema reusable.
- **SSE** (thay vì WebSocket): Dashboard chỉ cần server → client one-way. SSE đơn giản hơn, tự reconnect. WebSocket chỉ cần nếu thêm chat feature.

---

## 2. Architecture

### 2.1 Pragmatic Hexagonal (Ports & Adapters) — Backend

**Hexagonal** cho 4 services có domain logic phức tạp. **Layered** cho 3 services đơn giản.

**Dependency Rule:**

```
adapter/in (web, kafka) → application (ports/in) → domain (pure Java)
adapter/out (persistence, messaging, cache) → application (ports/out) → domain

domain:      ZERO external dependencies (no Spring, no JPA, no Kafka)
application: depends only on domain
adapter:     depends on application + infrastructure libs
config:      Spring wiring — connects adapters to ports
```

**Hexagonal Service Structure (attendance-service, auth-service, admin-service):**

```
attendance-service/
├── domain/                              # ① DOMAIN CORE — pure Java, zero dependencies
│   ├── model/
│   │   ├── AttendanceRecord.java        # Rich domain entity (business methods)
│   │   ├── Location.java
│   │   ├── Shift.java
│   │   └── Timesheet.java
│   ├── valueobject/
│   │   ├── BssidSignal.java             # Immutable value objects (Java record)
│   │   ├── GpsCoordinate.java
│   │   ├── FraudScore.java
│   │   └── TimesheetPeriod.java
│   ├── event/
│   │   ├── CheckInCompleted.java        # Domain events
│   │   ├── DuplicateCheckInRejected.java
│   │   └── TimesheetLocked.java
│   ├── exception/
│   │   ├── DuplicateCheckInException.java
│   │   └── InvalidLocationException.java
│   └── service/                         # Domain services (pure business logic)
│       ├── DuplicateCheckInGuard.java
│       ├── LocationVerifier.java
│       ├── FraudScorer.java
│       └── ShiftRuleEngine.java
│
├── application/                         # ② APPLICATION — use cases, orchestration
│   ├── port/
│   │   ├── in/                          # Inbound ports (use case interfaces)
│   │   │   ├── CheckInUseCase.java
│   │   │   ├── CheckOutUseCase.java
│   │   │   ├── ViewHistoryUseCase.java
│   │   │   └── ManageTimesheetUseCase.java
│   │   └── out/                         # Outbound ports (infrastructure interfaces)
│   │       ├── AttendanceRepository.java
│   │       ├── LocationRepository.java
│   │       ├── EmployeeRepository.java
│   │       ├── EventPublisher.java
│   │       ├── CachePort.java
│   │       └── NotificationPort.java
│   ├── service/                         # Use case implementations
│   │   ├── CheckInService.java
│   │   ├── TimesheetService.java
│   │   └── ShiftService.java
│   └── dto/                             # Application-level commands & queries
│       ├── CheckInCommand.java
│       ├── CheckInResult.java
│       └── AttendanceQuery.java
│
├── adapter/                             # ③ ADAPTERS — infrastructure implementations
│   ├── in/                              # Inbound adapters (driving side)
│   │   ├── web/
│   │   │   ├── CheckInController.java
│   │   │   ├── TimesheetController.java
│   │   │   ├── request/
│   │   │   └── response/
│   │   └── kafka/
│   │       └── ImportEventListener.java
│   │
│   └── out/                             # Outbound adapters (driven side)
│       ├── persistence/
│       │   ├── entity/                  # JPA entities (NOT domain model)
│       │   ├── repository/              # Spring Data JPA repos
│       │   ├── mapper/                  # JPA Entity ↔ Domain Model (MapStruct)
│       │   └── AttendanceRepositoryAdapter.java
│       ├── messaging/
│       │   └── KafkaEventPublisher.java
│       ├── cache/
│       │   ├── CaffeineCacheAdapter.java
│       │   └── RedisCacheAdapter.java
│       └── notification/
│           ├── NtfyNotificationAdapter.java       # Local dev
│           └── FcmNotificationAdapter.java        # Production
│
└── config/                              # ④ SPRING CONFIG
    ├── DataSourceConfig.java
    ├── KafkaConfig.java
    ├── SecurityConfig.java
    └── BeanConfig.java
```

**Layered Service Structure (gateway, notification-service, report-service):**

```
notification-service/
├── config/
├── controller/
├── service/
│   ├── NotificationDispatcher.java
│   ├── NtfyPushService.java
│   ├── FcmPushService.java
│   ├── MailHogEmailService.java
│   └── SesEmailService.java
├── listener/
│   └── NotificationKafkaListener.java
├── model/
└── dto/
```

### 2.2 Domain Model vs JPA Entity — Separation

Domain models là rich objects có business methods. JPA entities chỉ mapping database.

```
// ① Domain Model (domain/model/) — pure Java, có business logic
AttendanceRecord {
  markSuspicious(FraudScore score)
  isWithinGracePeriod(Shift shift)
  belongsToShift(Shift shift)
}

Timesheet {
  lock(Employee approver)          // throws if status != APPROVED
  addRecord(AttendanceRecord rec)  // validates business rules
  calculateOvertime(ShiftRules rules)
}

// ② JPA Entity (adapter/out/persistence/entity/) — chỉ mapping DB
AttendanceJpaEntity {
  @Id, @Column, @ManyToOne...      // JPA annotations only
}

// ③ MapStruct mapper chuyển đổi giữa hai layers
@Mapper
AttendanceMapper {
  AttendanceRecord toDomain(AttendanceJpaEntity entity)
  AttendanceJpaEntity toJpa(AttendanceRecord domain)
}
```

### 2.3 Architecture Enforcement — ArchUnit

```
// Chạy như unit test trong CI — fail build nếu vi phạm
@ArchTest domainShouldNotDependOnSpring:
  classes in "..domain.." should not depend on "org.springframework.."

@ArchTest domainShouldNotDependOnJpa:
  classes in "..domain.." should not depend on "jakarta.persistence.."

@ArchTest applicationShouldNotDependOnAdapter:
  classes in "..application.." should not depend on "..adapter.."

@ArchTest adapterInShouldOnlyAccessPortIn:
  classes in "..adapter.in.." should only access "..application.port.in.."

@ArchTest adapterOutShouldImplementPortOut:
  classes in "..adapter.out.." should implement "..application.port.out.."
```

### 2.4 Architecture Decision Summary

| Service | Architecture | Lý do |
|---------|-------------|-------|
| **attendance-service** | Hexagonal | Domain logic phức tạp: location verify, duplicate guard, fraud scoring, shift rules, timesheet lifecycle, late grace allowance |
| **auth-service** | Hexagonal | Domain logic: 2FA flow, OTP lifecycle, device attestation, session management |
| **admin-service** | Hexagonal | Domain logic: Excel import pipeline, maker-checker, org tree rules |
| **ai-service** | Hexagonal | Domain logic: Anomaly detection scoring, NLP chatbot, knowledge base Q&A |
| **gateway** | Layered | Chỉ routing + filter, không có domain logic |
| **notification-service** | Layered | Kafka consumer → dispatch to channels |
| **report-service** | Layered | Kafka consumer → query → generate → upload |

### 2.5 Feature-Based Architecture — Frontend

Both Web and Mobile use Feature-Based structure (not Hexagonal — React has natural patterns with hooks/components).

**Mobile:**

```
mobile/src/
├── app/                                # Navigation, providers
│   ├── navigation/
│   │   ├── AuthStack.tsx
│   │   ├── MainTab.tsx
│   │   └── ModalStack.tsx
│   └── providers/
│
├── features/                           # ★ Mỗi feature là 1 mini-module
│   ├── auth/
│   │   ├── screens/                    # LoginScreen, TwoFactorScreen
│   │   ├── components/                 # LoginForm, OtpInput, BiometricPrompt
│   │   ├── hooks/                      # useAuth, useLogin, useVerify2fa
│   │   ├── api/                        # authApi.ts
│   │   ├── stores/                     # authStore.ts (Zustand)
│   │   └── index.ts
│   ├── checkin/
│   │   ├── screens/                    # CheckInScreen
│   │   ├── components/                 # CheckInButton, StatusCard, LocationInfo
│   │   ├── hooks/                      # useCheckIn, useLocationCollector
│   │   ├── api/
│   │   ├── services/                   # wifiScanner.ts, gpsService.ts
│   │   └── index.ts
│   ├── history/
│   ├── leave/
│   ├── schedule/
│   └── profile/
│
├── shared/                             # Không chứa business logic
│   ├── components/
│   ├── hooks/
│   ├── api/                            # axiosInstance.ts
│   ├── services/                       # secureStorage, biometric, backgroundSync
│   ├── db/                             # WatermelonDB schema, sync protocol
│   ├── types/
│   ├── utils/
│   └── constants/
│
└── __tests__/                          # E2E (Detox)
```

**Web Dashboard:**

```
web/src/
├── app/                                # Routing, providers, layouts
│   ├── routes.tsx
│   ├── providers/
│   └── layouts/
│       ├── MainLayout.tsx              # Ant Design: Sider + Header + Content
│       ├── AuthLayout.tsx
│       └── ErrorBoundary.tsx
│
├── features/                           # ★ Mỗi feature là 1 mini-module
│   ├── auth/                           # LoginForm, useAuth, usePermission, authStore
│   ├── dashboard/                      # StatCards, AttendanceChart, RealtimeGauge
│   ├── attendance/                     # AttendanceTable, MonitorFeed
│   ├── timesheet/                      # TimesheetGrid, ReviewPanel, LockConfirm
│   ├── leave/                          # LeaveRequestForm, ApprovalQueue
│   ├── admin/                          # OrgTree, UserTable, ImportWizard, MakerChecker
│   ├── reports/                        # ReportBuilder, ExportButton
│   └── notification/                   # NotificationList, NotificationBadge
│
├── shared/                             # PermissionGate, PageHeader, DataTable
│   ├── components/
│   ├── hooks/
│   ├── api/
│   ├── types/
│   ├── utils/
│   ├── constants/
│   └── stores/                         # uiStore (sidebar, theme)
│
├── pages/                              # Thin wrappers — compose features
│   ├── auth/                           # LoginPage, TwoFactorPage
│   ├── dashboard/                      # DashboardPage
│   ├── attendance/                     # AttendanceMonitorPage, HistoryPage
│   ├── timesheet/                      # TimesheetListPage, ReviewPage, LockPage
│   ├── leave/                          # LeaveRequestListPage, DetailPage
│   ├── admin/                          # UserMgmt, OrgStructure, LocationConfig, ShiftRule, AuditLog
│   ├── reports/                        # ReportListPage, ReportBuilderPage
│   └── notification/                   # NotificationCenterPage
│
└── __tests__/                          # E2E (Playwright)
```

### 2.6 Frontend Import Rules

```
pages/     → features/ + shared/          ✅ pages compose features
features/  → shared/                      ✅ features dùng shared utils
features/  ✗ features/                    ❌ features KHÔNG import lẫn nhau
shared/    ✗ features/                    ❌ shared KHÔNG depend on features
shared/    ✗ pages/                       ❌ shared KHÔNG depend on pages
```

**ESLint enforcement** (`eslint-plugin-boundaries`):
```json
{
  "plugins": ["boundaries"],
  "settings": {
    "boundaries/elements": [
      { "type": "app",      "pattern": "app/*" },
      { "type": "features", "pattern": "features/*" },
      { "type": "shared",   "pattern": "shared/*" },
      { "type": "pages",    "pattern": "pages/*" }
    ]
  },
  "rules": {
    "boundaries/element-types": [2, {
      "default": "disallow",
      "rules": [
        { "from": "pages",    "allow": ["features", "shared"] },
        { "from": "features", "allow": ["shared"] },
        { "from": "shared",   "allow": ["shared"] },
        { "from": "app",      "allow": ["features", "shared", "pages"] }
      ]
    }]
  }
}
```

### 2.7 Web RBAC Pattern

```
// shared/components/PermissionGate.tsx
<PermissionGate action="approve" resource="timesheet" scope="department">
  <ApproveButton onClick={handleApprove} />
</PermissionGate>

// app/routes.tsx — route guard
<Route element={<RequirePermission action="view" resource="admin" />}>
  <Route path="/admin/*" element={<AdminLayout />} />
</Route>

// features/auth/hooks/usePermission.ts
canAccess({
  action: 'export',       // view|create|edit|approve|delete|export|lock
  resource: 'attendance', // attendance|timesheet|leave|admin|report
  scope: 'branch',        // self|unit|department|branch|region|all
})
```

---

## 3. Monorepo Structure

```
smart-attendance/
├── docker-compose.yml
├── docker-compose.infra.yml
├── .env.example
├── docs/
│   └── ARCHITECTURE.md              # This file
├── CLAUDE.md                        # Quick reference for Claude Code
│
├── backend/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gateway/                     # [Layered]
│   ├── auth-service/                # [Hexagonal] domain/ application/ adapter/ config/
│   ├── attendance-service/          # [Hexagonal] domain/ application/ adapter/ config/
│   ├── admin-service/               # [Hexagonal] domain/ application/ adapter/ config/
│   ├── ai-service/                  # [Hexagonal] Python FastAPI — anomaly detection, chatbot
│   ├── notification-service/        # [Layered] config/ listener/ service/ dto/
│   ├── report-service/              # [Layered] config/ listener/ service/ dto/
│   └── common/                      # Shared DTOs, security, events, utils
│
├── mobile/                          # [Feature-Based] features/ shared/ app/
├── web/                             # [Feature-Based] features/ shared/ pages/ app/
│
└── infra/
    ├── postgres/
    ├── redis/
    ├── kafka/                       # KRaft mode (no ZooKeeper)
    └── nginx/
```

---

## 4. Infrastructure

### 4.1 Microservices & Ports

| Service | Port | Responsibility |
|---------|------|---------------|
| gateway | 8080 | API routing, rate limit, JWT filter |
| auth-service | 8081 | Login, JWT, 2FA/TOTP, OTP delivery |
| attendance-service | 8082 | Check-in/out, location verify, duplicate guard, shift, timesheet, late grace, dashboard SSE |
| admin-service | 8083 | User/org/location CRUD, Excel import, maker-checker, WiFi survey |
| ai-service | 8086 | Anomaly detection, AI Chatbot NLP, knowledge base Q&A |
| notification-service | 8084 | Kafka consumer → push (ntfy), email (MailHog), SMS mock |
| report-service | 8085 | Kafka consumer → async report gen, dashboard metrics |

### 4.2 Infrastructure Ports (Docker Compose)

| Service | Port | UI |
|---------|------|-----|
| PostgreSQL Master | 5432 | — |
| PostgreSQL Slave | 5433 | — |
| Redis | 6379 | — |
| Kafka | 29092 (external), 9092 (internal) | — |
| Kafka UI | 8090 | http://localhost:8090 |
| ntfy (Push) | 8095 | http://localhost:8095 |
| MailHog (Email/SMS mock) | 1025 (SMTP), 8025 (UI) | http://localhost:8025 |
| MinIO | 9000 (API), 9001 (Console) | http://localhost:9001 |

### 4.3 Kafka Topics & Consumer Groups

| Topic | Producer | Consumers | Purpose |
|-------|----------|-----------|---------|
| `attendance.checkin` | attendance-service | notification-service, report-service, ai-service | Check-in/out events |
| `attendance.escalation` | attendance-service | notification-service | Late/absent escalation |
| `attendance.notification` | admin-service, attendance-service | notification-service | Generic notifications |
| `attendance.audit` | all services | audit-service | Append-only audit trail |
| `attendance.import` | admin-service | admin-service | Excel import job progress |
| `attendance.anomaly` | ai-service | notification-service, attendance-service | Anomaly detection results (risk score, flags) |

KRaft mode (no ZooKeeper). Partition key: `employeeId`.

### 4.4 Database Design

- PostgreSQL 16 + PostGIS + pgvector. `attendance_records` partitioned by month.
- Key tables: `organizations`, `locations`, `wifi_access_points`, `employees`, `attendance_records`, `timesheets`, `audit_logs`, `anomaly_scores`, `chatbot_sessions`, `policy_embeddings`, `late_grace_quota`, `late_grace_config`, `config_changes`.
- Dashboard pre-aggregated tables: `report_attendance_daily`, `report_attendance_weekly`, `report_attendance_monthly`, `report_kpi_daily` (nightly batch job từ report-service Kafka consumer).
- 2FA fields: `two_factor_enabled`, `two_factor_method`, `totp_secret` (encrypted).
- Master-Slave routing: `@Transactional(readOnly=true)` → Slave; `@Transactional` → Master.
- Flyway migrations.

### 4.5 Spring Profiles Strategy

```
application.yml              # Shared defaults
application-local.yml        # IntelliJ/Gradle bootRun
application-docker.yml       # Docker Compose
application-staging.yml      # Staging
application-prod.yml         # Production — secrets từ Vault/AWS SSM
application-test.yml         # JUnit — Testcontainers
```

---

## 5. Push Notification (ntfy)

Local dev dùng **ntfy** — push notification server mã nguồn mở, chạy hoàn toàn trong Docker.

- Backend gửi push: `POST http://ntfy:80/sa-{employeeCode}` với headers `Title`, `Priority`, `Tags`.
- Mobile subscribe: `EventSource("http://host:8095/sa-{employeeCode}/sse")`.
- Production swap: đổi outbound adapter, port interface giữ nguyên.

```
NotificationPort (outbound port interface)
├── NtfyNotificationAdapter      (local dev)
└── FcmNotificationAdapter       (production)
```

## 6. 2FA / OTP

### TOTP (Google Authenticator)
- Library: `dev.samstevens.totp:totp:1.7.1`.
- Setup: generate secret → QR code URI → user scan → verify → enable 2FA.
- TOTP logic in `auth-service/domain/service/TotpService.java` — pure Java.

### Email/SMS OTP (fallback)
- 6-digit OTP, Redis TTL 5 min, max 3 attempts.
- Local: MailHog SMTP (port 1025), view at http://localhost:8025.

### Login Flow
1. `POST /api/v1/auth/login` → verify password → if 2FA → `partialToken` + `2FA_REQUIRED`.
2. `POST /api/v1/auth/verify-2fa` → verify TOTP/OTP → full JWT.
3. `POST /api/v1/auth/request-otp` → send OTP via SMS/Email.

```
OtpDeliveryPort (outbound port)
├── MailHogOtpAdapter     (local dev)
└── TwilioSesOtpAdapter   (production)
```

---

## 7. Development Setup Guide

### 7.1 Prerequisites

| Tool | Version | Check | Note |
|------|---------|-------|------|
| Docker Desktop | 24+ | `docker --version` | 4GB+ RAM in Settings → Resources |
| Docker Compose | v2+ | `docker compose version` | Included with Docker Desktop |
| Java JDK | 21+ | `java --version` | Eclipse Temurin or GraalVM |
| Gradle | 8.5+ | `./gradlew --version` | Wrapper included |
| Node.js | 20 LTS | `node --version` | Use `nvm` |
| React Native CLI | latest | `npx react-native --version` | `npm install -g react-native-cli` |
| Android Studio | latest | — | Android emulator + SDK |
| Xcode | 15+ | `xcode-select --version` | macOS only |
| Git | 2.40+ | `git --version` | — |

**Minimum system**: 16 GB RAM, 20 GB disk, macOS 13+ / Windows 10 WSL2 / Ubuntu 22.04.

### 7.2 Step 1 — Clone & Config

```bash
git clone https://github.com/hdbank/smart-attendance.git
cd smart-attendance && cp .env.example .env
```

### 7.3 Step 2 — Start Infrastructure

```bash
docker compose -f docker-compose.infra.yml up -d

# Verify each service:
docker exec sa-postgres-master pg_isready -U sa_admin -d smart_attendance
docker exec sa-redis redis-cli -a localdev123 ping
docker exec sa-kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 2>&1 | head -1
curl -s http://localhost:9000/minio/health/live
curl -d "Test notification" http://localhost:8095/sa-test
```

### 7.4 Step 3 — Start Backend

**Option A: Docker Compose**
```bash
docker compose up -d --build
docker compose logs -f gateway auth-service attendance-service
```

**Option B: Gradle (for dev/debug)**
```bash
cd backend
./gradlew :gateway:bootRun -Dspring.profiles.active=docker
./gradlew :auth-service:bootRun -Dspring.profiles.active=docker -Dserver.port=8081
./gradlew :attendance-service:bootRun -Dspring.profiles.active=docker -Dserver.port=8082
./gradlew :admin-service:bootRun -Dspring.profiles.active=docker -Dserver.port=8083
./gradlew :notification-service:bootRun -Dspring.profiles.active=docker -Dserver.port=8084
./gradlew :report-service:bootRun -Dspring.profiles.active=docker -Dserver.port=8085

# ai-service (Python):
cd ai-service && pip install -r requirements.txt && uvicorn main:app --port 8086
```

**Verify:**
```bash
for port in 8080 8081 8082 8083 8084 8085 8086; do
  echo "Port $port: $(curl -s http://localhost:$port/actuator/health | grep -o '"status":"[^"]*"')"
done
```

**Tests:**
```bash
cd backend && ./gradlew test
./gradlew :attendance-service:test
./gradlew test jacocoTestReport
```

### 7.5 Step 4 — Start Web Dashboard

```bash
cd web && npm install && npm run dev    # http://localhost:5173
# Or: docker compose up -d web          # http://localhost:3000
```

### 7.6 Step 5 — Start Mobile App

**Android Emulator:**
```bash
cd mobile && npm install
npx react-native start --reset-cache
npx react-native run-android
# API: http://10.0.2.2:8080
```

**iOS Simulator (macOS):**
```bash
cd mobile && npm install && cd ios && pod install && cd ..
npx react-native start --reset-cache
npx react-native run-ios --simulator="iPhone 15 Pro"
# API: http://localhost:8080
```

**Physical device:**
```bash
adb reverse tcp:8080 tcp:8080          # Android
adb reverse tcp:8095 tcp:8095
```

### 7.7 Step 6 — Verify Full System

```bash
docker compose ps
for port in 8080 8081 8082 8083 8084 8085 8086; do
  status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health)
  echo "localhost:$port → HTTP $status"
done
docker exec sa-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
curl -H "Title: Test" -d "System OK" http://localhost:8095/sa-test
```

### 7.8 Test Accounts (Local Dev)

| Role | Username | Password |
|------|----------|----------|
| System Admin | `admin@hdbank.vn` | `Admin@123` |
| Branch Manager | `manager.q1@hdbank.vn` | `Manager@123` |
| Employee (nghiệp vụ) | `nv001@hdbank.vn` | `Employee@123` |
| Employee (IT) | `it001@hdbank.vn` | `Employee@123` |

> Created by Flyway migration `V999__test_data.sql`. Docker profile only.

### 7.9 Troubleshooting

**Kafka**: `docker compose logs kafka` — "Cluster ID mismatch" → `docker compose down -v && docker compose up -d kafka`

**PostgreSQL Slave**: `docker exec sa-postgres-master psql -U sa_admin -d smart_attendance -c "SELECT * FROM pg_stat_replication;"` — no rows → `docker compose restart postgres-slave`

**Port conflict**: `lsof -i :5432` / `lsof -i :8080` → change port in `.env`

**Mobile API connection**: Android emulator uses `10.0.2.2`, iOS uses `localhost`. Physical device: `adb reverse tcp:8080 tcp:8080`

**MailHog**: Check http://localhost:8025 for OTP emails.

**Full cleanup**: `docker compose down -v --rmi local`

### 7.10 Startup Order

```
Step 1: Infrastructure
  postgres-master → postgres-slave, redis, kafka → kafka-ui, minio, ntfy, mailhog

Step 2: Backend Services
  gateway, auth-service, attendance-service, admin-service, ai-service, notification-service, report-service

Step 3: Frontend
  web (depends: gateway), mobile (depends: gateway + emulator)
```

---

## 8. Production Swap Guide

| Local Dev | Production |
|-----------|-----------|
| Apache Kafka (Docker KRaft) | Amazon MSK / Confluent Cloud |
| ntfy (self-hosted) | Firebase Cloud Messaging (FCM) |
| MailHog (mock SMTP) | AWS SES / SendGrid |
| Mock SMS (MailHog) | Twilio / FPT SMS Gateway |
| TOTP (java-totp) | Same library (no change) |
| MinIO (local S3) | AWS S3 / GCP Cloud Storage |
| PostgreSQL (Docker) | Amazon RDS / Cloud SQL |
| Redis (Docker) | Amazon ElastiCache / Upstash |
| MLflow (local) | MLflow on ECS / SageMaker Model Registry |
| scikit-learn (local training) | Same library + scheduled Celery worker on dedicated instance |

---

*End of Architecture & Development Guide*
