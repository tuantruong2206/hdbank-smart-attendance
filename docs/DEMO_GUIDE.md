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
