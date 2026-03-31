# Smart Attendance — Demo Guide

## Test Accounts (24 users)

### Management Accounts

| Role | Email | Password | Name | Location |
|------|-------|----------|------|----------|
| System Admin | `admin@hdbank.vn` | `Admin@123` | Nguyễn Văn Admin | HO |
| CEO | `ceo@hdbank.vn` | `Admin@123` | Nguyễn Đức CEO | HO |
| Division Director | `director.cntt@hdbank.vn` | `Manager@123` | Trương Minh Director | HO - Khối CNTT |
| Region Director | `region.hcm@hdbank.vn` | `Manager@123` | Hoàng Thị Region Director | Vùng HCM |
| Branch Manager | `manager.q1@hdbank.vn` | `Manager@123` | Trần Thị Manager | CN Quận 1 |
| Deputy Head | `deputy.q1@hdbank.vn` | `Manager@123` | Võ Văn Deputy | CN Quận 1 |
| Unit Head | `unit.web@hdbank.vn` | `Manager@123` | Đặng Quang Unit Head | HO - BP Web |

### Employee Accounts — Branch Q1 (nghiệp vụ, ca sáng 08:00-17:00)

| Code | Email | Name | Notable Status |
|------|-------|------|---------------|
| NV001 | `nv001@hdbank.vn` | Lê Văn Nhân Viên | Normal - 3/4 late grace used |
| NV002 | `nv002@hdbank.vn` | Phan Thị Hoa | Had SUSPICIOUS check-in (GPS mismatch + VPN) |
| NV003 | `nv003@hdbank.vn` | Ngô Thanh Tùng | Had OFFLINE check-in (WiFi down) |
| NV004 | `nv004@hdbank.vn` | Trần Minh Đức | Frequently late, **ALL 4 late grace EXHAUSTED** |
| NV005 | `nv005@hdbank.vn` | Lý Thị Mai | Had early leave (16:30) |
| NV006 | `nv006@hdbank.vn` | Huỳnh Văn Bảo | **FRAUD detected** — mock location, score 92 |
| NV007 | `nv007@hdbank.vn` | Đỗ Thị Lan | Uses QR code check-in |

> All employee passwords: `Employee@123`

### Employee Accounts — HO IT (kỹ thuật, multi-location)

| Code | Email | Name | Notable Status |
|------|-------|------|---------------|
| IT001 | `it001@hdbank.vn` | Phạm Minh IT | Multi-location: HO morning, CN Q1 afternoon |
| IT002 | `it002@hdbank.vn` | Bùi Quang Dev | OT worker (stayed till 19:00) |
| IT003 | `it003@hdbank.vn` | Vũ Thị QA | Normal |
| IT004 | `it004@hdbank.vn` | Lê Hoàng DevOps | Works on Floor 3 |
| IT005 | `it005@hdbank.vn` | Trịnh Minh BA | Normal |

---

## Demo Scenarios

### Viewing Backend Logs (Recommended During Demo)

Open a **separate terminal** to watch real-time backend logs while testing:

```bash
cd ~/Desktop/AI-HOME/claude_code/smart_attendance

# Watch check-in/out, timesheet, leave activity
docker compose logs -f attendance-service

# Watch login, 2FA, auth events
docker compose logs -f auth-service

# Watch all key services at once
docker compose logs -f gateway auth-service attendance-service notification-service

# Watch push/email notifications being sent
docker compose logs -f notification-service
```

> Press **Ctrl+C** to stop. No restart needed — logs are streamed live from running containers.

This is especially useful for scenarios 1-6 (check-in), 8 (chatbot), 12 (SLA), and 17 (circuit breaker).

---

### Mobile App Setup (Before Running Mobile Scenarios)

**Emulator (iOS/Android)**:
```bash
cd mobile && npm install --legacy-peer-deps && npx expo start --port 19000
# Press 'i' for iOS Simulator or 'a' for Android Emulator
```

**Physical Device (iPhone/Android)**:
1. Install **Expo Go** from App Store / Play Store
2. Phone and Mac must be on **same WiFi network**
3. Find Mac's IP: `ipconfig getifaddr en0` (current: `10.8.48.247`)
4. The API URL is configured in `mobile/src/shared/api/axiosInstance.ts` — update the IP if yours differs
5. Verify gateway is reachable: `curl http://YOUR_MAC_IP:8080/actuator/health`
6. Start Expo and scan QR code:
   ```bash
   cd mobile && npx expo start --port 19000
   ```

> **If login fails on physical device**: check that the IP in `axiosInstance.ts` matches your Mac's current IP. Run `ipconfig getifaddr en0` to verify.

---

### Scenario 1: Employee Daily Check-in (Mobile App)

**Login as**: `nv001@hdbank.vn` / `Employee@123`

1. See home screen with greeting, stats (22 days worked, 3 late, 11 leave days remaining)
2. See late grace indicator: "3/4 used" (yellow warning)
3. Tap **Chấm công** tab → see shift info (08:00-17:00)
4. Tap **Chấm công VÀO** → WiFi BSSID detected → "Chấm công vào thành công"
5. At end of day, tap **Chấm công RA**
6. Check **Lịch sử** tab → see this week's attendance records

**Key point**: Demonstrates WiFi-based location verification, shift display, late grace quota.

---

### Scenario 2: Late Employee — Grace Quota Warning (Mobile App)

**Login as**: `nv004@hdbank.vn` / `Employee@123`

1. Home screen shows: late count = 5, late grace = **0/4 remaining** (RED indicator)
2. This employee has used all 4 late grace allowances this month
3. Any future late arrival will be counted as "trễ không phép" (unexcused late)
4. Check history → see multiple late check-ins (08:25, 08:30)

**Key point**: Late grace quota system — configurable per org unit, auto-reset monthly.

---

### Scenario 3: Suspicious Check-in — Fraud Detection (Web Dashboard)

**Login as**: `manager.q1@hdbank.vn` / `Manager@123`

1. Dashboard shows: 1-2 suspicious records this week
2. Go to **Chấm công** (Attendance Monitor)
3. Filter by status → see NV002's record flagged SUSPICIOUS (score 75):
   - GPS location 2.3km from office
   - VPN detected
4. See NV006's record flagged SUSPICIOUS (score 92):
   - **Mock location app detected**
   - **Rooted device**
   - Auto-escalated to management

**Key point**: AI anomaly detection, fraud scoring 0-100, auto-escalation at score ≥90.

---

### Scenario 4: Leave Request Approval Workflow (Web Dashboard)

**Login as**: `manager.q1@hdbank.vn` / `Manager@123`

1. Dashboard shows: **3 pending approvals**
2. Go to **Nghỉ phép** (Leave)
3. See pending requests:
   - NV002 (Phan Thị Hoa): Annual leave Apr 7-8, "Có việc cá nhân"
   - NV005 (Lý Thị Mai): Sick leave Mar 31, "Bị cảm sốt, có giấy bác sĩ"
   - NV003 (Ngô Thanh Tùng): Personal leave Apr 10, "Đưa con đi khám bệnh"
4. Approve/reject with comments
5. Check already processed: NV004 approved (Apr 1-3), NV006 rejected (conflicted with audit schedule)

**Key point**: Multi-level approval, SLA tracking, approval history with comments.

---

### Scenario 5: IT Employee Multi-Location Check-in (Mobile App)

**Login as**: `it001@hdbank.vn` / `Employee@123`

1. As IT employee, can check in at **multiple locations** (unlike nghiệp vụ staff)
2. History shows: Mar 25 — checked in at HO Floor 2 (morning), then CN Q1 (afternoon)
3. Minimum interval between check-ins: 30 minutes (configurable)
4. Different WiFi BSSID detected at each location

**Key point**: Employee type distinction — nghiệp vụ (fixed, 1 check-in/shift) vs IT (multi-location, interval-based).

---

### Scenario 6: Offline Check-in & Sync (Mobile App)

**Login as**: `nv003@hdbank.vn` / `Employee@123`

1. Check history → Mar 26 shows status "OFFLINE_SYNCED"
2. This happened when WiFi was down at the office
3. The app cached the check-in locally with GPS verification
4. When connectivity restored, it synced with UUID dedup protection
5. Verification method shows "GPS" instead of "WIFI"

**Key point**: Offline-first architecture, secure timestamps, UUID dedup, sync within 24h.

---

### Scenario 7: Timesheet Review & Lock (Web Dashboard)

**Login as**: `manager.q1@hdbank.vn` / `Manager@123`

1. Go to timesheet section
2. See February 2026 timesheets: all LOCKED (finalized)
3. See March 2026 timesheets in various states:
   - NV001: DRAFT (19 work days, 2 late, 1h OT)
   - NV002: PENDING_REVIEW (18 work days, 1 late, 1 absent)
   - NV004: DRAFT (17 work days, **5 late**, 2 absent — problematic employee)
4. Review and approve timesheets
5. After approval, System Admin can lock (creates immutable snapshot)

**Key point**: Timesheet lifecycle: Draft → Pending Review → Approved → Locked (snapshot).

---

### Scenario 8: AI Chatbot — Vietnamese NLP (Mobile App or API)

**Test via API**:
```bash
# Ask about leave balance (Vietnamese)
curl -s -X POST http://localhost:8080/api/v1/ai/chat/message \
  -H "Content-Type: application/json" \
  -d '{"employee_id":"e0000000-0000-0000-0000-000000000003","message":"còn bao nhiêu ngày phép?"}' | jq .

# Ask about late count
curl -s -X POST http://localhost:8080/api/v1/ai/chat/message \
  -H "Content-Type: application/json" \
  -d '{"employee_id":"e0000000-0000-0000-0000-000000000003","message":"tháng này tôi đi trễ mấy lần?"}' | jq .

# Request leave via natural language
curl -s -X POST http://localhost:8080/api/v1/ai/chat/message \
  -H "Content-Type: application/json" \
  -d '{"employee_id":"e0000000-0000-0000-0000-000000000003","message":"xin nghỉ phép ngày mai"}' | jq .

# Ask about schedule
curl -s -X POST http://localhost:8080/api/v1/ai/chat/message \
  -H "Content-Type: application/json" \
  -d '{"employee_id":"e0000000-0000-0000-0000-000000000003","message":"ca làm việc của tôi là gì?"}' | jq .
```

**Key point**: Vietnamese NLP intent detection, auto-create leave from natural language, RBAC-enforced (employees only see own data).

---

### Scenario 9: Executive Dashboard (Web Dashboard)

**Login as**: `ceo@hdbank.vn` / `Admin@123`

1. See organization-wide KPIs:
   - Attendance rate: 87-100% across the week
   - On-time rate: 83-100%
   - Average work hours: ~8.2h
   - OT hours trend
2. Branch comparison metrics
3. Suspicious activity alerts
4. Real-time employee count (SSE)

**Key point**: Role-based dashboard — CEO sees all branches, manager sees own branch only.

---

### Scenario 10: System Admin Operations (Web Dashboard)

**Login as**: `admin@hdbank.vn` / `Admin@123`

1. **Quản trị → Nhân viên**: See all 24 employees, search/filter
2. **Quản trị → Tổ chức**: View org tree (HO → Khối → Phòng → Bộ phận, Vùng → CN → Phòng)
3. **Excel Import**: Upload employee data (template → validate → dry run → import)
4. **Maker-Checker**: Sensitive config changes require 2-person approval
5. **Audit Log**: All actions tracked immutably

**Key point**: Full admin CRUD, org hierarchy management, Excel import pipeline, maker-checker for sensitive changes.

---

### Scenario 11: Knowledge Base Q&A — AI Policy Chatbot (API)

**Test via API**:
```bash
# Ask about company attendance policy (Vietnamese)
curl -s -X POST http://localhost:8080/api/v1/ai/chat/message \
  -H "Content-Type: application/json" \
  -d '{"employee_id":"e0000000-0000-0000-0000-000000000003","message":"quy định chấm công là gì?"}' | jq .

# Ask about late grace policy
curl -s -X POST http://localhost:8080/api/v1/ai/chat/message \
  -H "Content-Type: application/json" \
  -d '{"employee_id":"e0000000-0000-0000-0000-000000000003","message":"quy định trễ có phép"}' | jq .

# Ask about OT rules
curl -s -X POST http://localhost:8080/api/v1/ai/chat/message \
  -H "Content-Type: application/json" \
  -d '{"employee_id":"e0000000-0000-0000-0000-000000000003","message":"hệ số làm thêm giờ bao nhiêu?"}' | jq .

# Ask about holidays
curl -s -X POST http://localhost:8080/api/v1/ai/chat/message \
  -H "Content-Type: application/json" \
  -d '{"employee_id":"e0000000-0000-0000-0000-000000000003","message":"ngày lễ năm nay"}' | jq .
```

**Expected**: Chatbot searches 8 HDBank policy documents in pgvector knowledge base and returns relevant policy content in Vietnamese.

**Key point**: RAG (Retrieval-Augmented Generation) using pgvector embeddings. When chatbot can't match a specific intent, it searches the knowledge base for relevant company policies.

---

### Scenario 12: Leave SLA Enforcement (Background)

**Setup**: Leave request pending for NV002 (`nv002@hdbank.vn`) since earlier today.

**How it works**:
1. LeaveSlaScheduler runs every hour
2. Checks all PENDING leave requests against SLA thresholds:
   - SICK leave: 4 hours
   - ANNUAL leave: 48 hours (2 business days)
   - PERSONAL leave: 24 hours
3. If SLA exceeded → sends HIGH priority reminder to current approver
4. If 2x SLA exceeded → escalates with LEAVE_SLA_ESCALATION audit event

**Verify**: Check MailHog at http://localhost:8025 for SLA reminder emails sent to manager.

**Key point**: Automated SLA enforcement prevents leave requests from being forgotten. Configurable per leave type.

---

### Scenario 13: Permission Matrix — Role-Based Access (Web Dashboard)

**Test with different roles**:

1. **Login as Employee** (`nv001@hdbank.vn` / `Employee@123`):
   - Dashboard shows: Employee widgets only (11 widgets)
   - Can see: own attendance, own leave, own timesheet
   - Cannot see: admin menu, team data, org-wide reports

2. **Login as Manager** (`manager.q1@hdbank.vn` / `Manager@123`):
   - Dashboard shows: Employee + Manager widgets (20 widgets)
   - Can see: team attendance, pending approvals, branch metrics
   - Can do: approve leave, approve timesheet
   - Cannot see: admin CRUD, org-wide data

3. **Login as Admin** (`admin@hdbank.vn` / `Admin@123`):
   - Dashboard shows: System Admin widgets (3 widgets)
   - Can see: all admin features, all data
   - Can do: lock timesheets, import employees, manage config

**Verify permissions API**:
```bash
# Get permissions for EMPLOYEE role
curl -s http://localhost:8080/api/v1/auth/permissions?role=EMPLOYEE | jq .

# Get permissions for SYSTEM_ADMIN
curl -s http://localhost:8080/api/v1/auth/permissions?role=SYSTEM_ADMIN | jq .
```

**Key point**: Full permission matrix (role × action × resource × scope) stored in DB, cached in-memory, enforced at endpoint + UI level via PermissionGate component.

---

### Scenario 14: Excel Import Pipeline (Web Dashboard)

**Login as**: `admin@hdbank.vn` / `Admin@123`

**Test via API**:
```bash
# Step 1: Download template
curl -s http://localhost:8080/api/v1/admin/import/template -o employee_template.xlsx

# Step 2: Upload file for validation
curl -s -X POST http://localhost:8080/api/v1/admin/import/upload \
  -F "file=@employee_template.xlsx" | jq .

# Step 3: Dry run (preview what will happen)
curl -s -X POST http://localhost:8080/api/v1/admin/import/dry-run \
  -F "file=@employee_template.xlsx" | jq .

# Step 4: Execute import
curl -s -X POST http://localhost:8080/api/v1/admin/import/execute \
  -F "file=@employee_template.xlsx" \
  -F "mode=UPSERT" | jq .

# Step 5: Rollback (within 24h)
curl -s -X POST http://localhost:8080/api/v1/admin/import/rollback/{batchId} | jq .
```

**Pipeline**:
1. **Template**: EasyExcel template with Vietnamese column headers (Mã NV, Họ tên, Email, etc.)
2. **Validate**: Check required fields, email format, org code exists, duplicate detection
3. **Dry Run**: Shows new/update/skip counts without touching DB
4. **Execute**: INSERT/UPDATE/UPSERT mode, tagged with batchId for rollback
5. **Rollback**: Soft-delete imported records within 24h window

**Key point**: Streaming Excel read (EasyExcel) handles 10,000+ rows without OOM. Batch tagging enables safe rollback.

---

### Scenario 15: Maker-Checker Workflow (Web Dashboard)

**Login as Admin 1** (`admin@hdbank.vn` / `Admin@123`):

**Test via API**:
```bash
# Step 1: Admin requests a config change (maker)
curl -s -X POST http://localhost:8080/api/v1/admin/config-changes \
  -H "Content-Type: application/json" \
  -H "X-User-Id: e0000000-0000-0000-0000-000000000001" \
  -d '{
    "entityType": "SHIFT",
    "entityId": "d0000000-0000-0000-0000-000000000001",
    "changeType": "UPDATE",
    "oldValue": {"gracePeriodMinutes": 15},
    "newValue": {"gracePeriodMinutes": 20}
  }' | jq .

# Step 2: Check pending changes
curl -s http://localhost:8080/api/v1/admin/config-changes/pending | jq .

# Step 3: Another admin approves (checker) — cannot self-approve
curl -s -X POST http://localhost:8080/api/v1/admin/config-changes/{id}/approve \
  -H "Content-Type: application/json" \
  -H "X-User-Id: e0000000-0000-0000-0000-000000000010" \
  -d '{"comment": "Đồng ý tăng grace period lên 20 phút"}' | jq .
```

**Rules**:
- Maker cannot approve their own change (self-approval prevention)
- Change is only applied after checker approval
- Rejected changes include reason
- Full history with audit trail

**Key point**: Sensitive configuration changes (shift rules, grace periods, org structure) require 2-person approval to prevent unauthorized modifications.

---

### Scenario 16: QR Code Check-in (API)

**Test via API**:
```bash
# Step 1: Generate QR code for a location (admin/manager)
curl -s -X POST http://localhost:8080/api/v1/attendance/qr/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"locationId": "b0000000-0000-0000-0000-000000000004"}' | jq .
# Returns: { "token": "abc123...", "expiresAt": "2026-03-31T10:15:00Z" }

# Step 2: Employee scans QR and checks in with the token
curl -s -X POST http://localhost:8080/api/v1/attendance/check-in \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{
    "employeeCode": "NV001",
    "employeeType": "NGHIEP_VU",
    "qrToken": "abc123...",
    "deviceId": "mobile-001"
  }' | jq .
```

**QR Code lifecycle**:
- Generated with SHA-256 token, TTL 5-10 minutes
- Stored in Redis with expiry
- One-time use: token deleted after successful check-in
- Auto-rotate: generate new QR when old one expires

**Key point**: Alternative check-in method when WiFi/GPS unavailable. Tokens are cryptographically generated, time-limited, and single-use.

---

### Scenario 17: Circuit Breaker — Graceful Degradation (Background)

**How to observe**:

1. Stop ntfy container to simulate push notification failure:
   ```bash
   docker stop sa-ntfy
   ```

2. Trigger a check-in that would normally send a push notification:
   ```bash
   curl -s -X POST http://localhost:8080/api/v1/attendance/check-in \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer TOKEN" \
     -d '{"employeeCode":"NV001","employeeType":"NGHIEP_VU","bssidSignals":[{"bssid":"AA:BB:CC:DD:EE:04","rssi":-45}],"deviceId":"test"}' | jq .
   ```

3. Check notification-service logs:
   ```bash
   docker compose logs notification-service --tail 20
   ```
   Expected: "Circuit breaker fallback for ntfy push" — push failed gracefully, email still sent

4. After 10+ failures, circuit opens — subsequent push attempts fail-fast (no waiting)

5. Restart ntfy:
   ```bash
   docker start sa-ntfy
   ```
   Circuit auto-recovers after 30s wait in half-open state.

**Configuration**:
- Sliding window: 10 calls
- Failure threshold: 50%
- Wait in open state: 30 seconds
- Retry: 3 attempts with 2s delay

**Key point**: Resilience4j prevents cascade failures. When ntfy is down, push fails fast but email/SMS still work. System self-heals when the service recovers.

---

## Data Summary

| Data Type | Count | Notes |
|-----------|-------|-------|
| Organizations | 7 | HO + branches, 4 levels deep |
| Locations | 4 | HO (3 floors) + CN Q1 |
| WiFi APs | 5 | 3 at HO, 2 at CN Q1 |
| Employees | 24 | 7 roles, 12 staff + 5 IT + 7 management |
| Shifts | 3 | Standard, Afternoon, Night |
| Attendance Records | 40+ | Mon-Fri Mar 24-28, various scenarios |
| Leave Requests | 6 | 1 approved, 3 pending, 1 rejected, 1 personal |
| Timesheets | 8 | Feb locked, Mar in progress |
| Anomaly Scores | 2 | Score 75 (GPS mismatch) + Score 92 (mock location) |
| Holidays | 4 | Hùng Vương, 30/4, 1/5, 2/9 |
| Policy Documents | 8 | Knowledge base: attendance, leave, OT, timesheet, escalation, holidays, security |
| Permissions | 70+ | Full role × action × resource × scope matrix |
| Demo Scenarios | 17 | Covers all major features end-to-end |
