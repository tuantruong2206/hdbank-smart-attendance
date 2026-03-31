-- Smart Attendance - Initial Schema
-- PostgreSQL 16 + PostGIS + pgvector

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Organizations (org tree)
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(50) NOT NULL,
    parent_id UUID REFERENCES organizations(id),
    level INTEGER NOT NULL DEFAULT 0,
    path TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Locations
CREATE TABLE locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(200) NOT NULL,
    address TEXT,
    building VARCHAR(100),
    floor INTEGER,
    gps_latitude DOUBLE PRECISION,
    gps_longitude DOUBLE PRECISION,
    geofence_radius_meters INTEGER DEFAULT 200,
    geofence_polygon GEOMETRY(POLYGON, 4326),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- WiFi Access Points
CREATE TABLE wifi_access_points (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    location_id UUID NOT NULL REFERENCES locations(id),
    bssid VARCHAR(17) NOT NULL,
    ssid VARCHAR(100),
    floor INTEGER,
    signal_zone VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(bssid, location_id)
);

-- Employees
CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_code VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(20),
    avatar_url TEXT,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    primary_location_id UUID REFERENCES locations(id),
    employee_type VARCHAR(20) NOT NULL DEFAULT 'NGHIEP_VU',
    role VARCHAR(50) NOT NULL DEFAULT 'EMPLOYEE',
    is_active BOOLEAN DEFAULT true,
    two_factor_enabled BOOLEAN DEFAULT false,
    two_factor_method VARCHAR(20),
    totp_secret VARCHAR(255),
    device_id VARCHAR(255),
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Refresh Tokens
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    token VARCHAR(500) UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Shifts
CREATE TABLE shifts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) UNIQUE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_overnight BOOLEAN DEFAULT false,
    grace_period_minutes INTEGER DEFAULT 15,
    early_departure_minutes INTEGER DEFAULT 15,
    ot_multiplier DECIMAL(3,2) DEFAULT 1.50,
    organization_id UUID REFERENCES organizations(id),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Employee Shift Assignments
CREATE TABLE employee_shifts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    shift_id UUID NOT NULL REFERENCES shifts(id),
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Attendance Records (partitioned by month)
CREATE TABLE attendance_records (
    id UUID NOT NULL DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL,
    employee_code VARCHAR(20) NOT NULL,
    check_type VARCHAR(10) NOT NULL,
    check_time TIMESTAMPTZ NOT NULL,
    location_id UUID,
    wifi_bssid VARCHAR(17),
    wifi_ssid VARCHAR(100),
    wifi_rssi INTEGER,
    gps_latitude DOUBLE PRECISION,
    gps_longitude DOUBLE PRECISION,
    gps_accuracy DOUBLE PRECISION,
    device_id VARCHAR(255),
    device_info JSONB,
    verification_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VALID',
    fraud_score INTEGER DEFAULT 0,
    fraud_flags JSONB,
    is_offline BOOLEAN DEFAULT false,
    offline_uuid UUID,
    shift_id UUID,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (id, check_time)
) PARTITION BY RANGE (check_time);

-- Partitions for 2026
CREATE TABLE attendance_records_2026_01 PARTITION OF attendance_records FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE attendance_records_2026_02 PARTITION OF attendance_records FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE attendance_records_2026_03 PARTITION OF attendance_records FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE attendance_records_2026_04 PARTITION OF attendance_records FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE attendance_records_2026_05 PARTITION OF attendance_records FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE attendance_records_2026_06 PARTITION OF attendance_records FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE attendance_records_2026_07 PARTITION OF attendance_records FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE attendance_records_2026_08 PARTITION OF attendance_records FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE attendance_records_2026_09 PARTITION OF attendance_records FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
CREATE TABLE attendance_records_2026_10 PARTITION OF attendance_records FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
CREATE TABLE attendance_records_2026_11 PARTITION OF attendance_records FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE attendance_records_2026_12 PARTITION OF attendance_records FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');

-- Timesheets
CREATE TABLE timesheets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    period_month INTEGER NOT NULL,
    period_year INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_work_days DECIMAL(5,2),
    total_late_count INTEGER DEFAULT 0,
    total_early_leave_count INTEGER DEFAULT 0,
    total_absent_count INTEGER DEFAULT 0,
    total_ot_hours DECIMAL(6,2) DEFAULT 0,
    total_late_grace_used INTEGER DEFAULT 0,
    approved_by UUID REFERENCES employees(id),
    approved_at TIMESTAMPTZ,
    locked_by UUID REFERENCES employees(id),
    locked_at TIMESTAMPTZ,
    snapshot JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(employee_id, period_month, period_year)
);

-- Leave Requests
CREATE TABLE leave_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_half VARCHAR(10),
    end_half VARCHAR(10),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    current_approver_id UUID REFERENCES employees(id),
    approval_level INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Leave Approvals
CREATE TABLE leave_approvals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    leave_request_id UUID NOT NULL REFERENCES leave_requests(id),
    approver_id UUID NOT NULL REFERENCES employees(id),
    level INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    comment TEXT,
    decided_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Leave Balances
CREATE TABLE leave_balances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    leave_type VARCHAR(50) NOT NULL,
    year INTEGER NOT NULL,
    total_days DECIMAL(5,2) NOT NULL,
    used_days DECIMAL(5,2) DEFAULT 0,
    UNIQUE(employee_id, leave_type, year)
);

-- Late Grace Quota
CREATE TABLE late_grace_quota (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    month INTEGER NOT NULL,
    year INTEGER NOT NULL,
    max_allowed INTEGER NOT NULL DEFAULT 4,
    used_count INTEGER DEFAULT 0,
    hr_override BOOLEAN DEFAULT false,
    hr_override_by UUID REFERENCES employees(id),
    hr_override_reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(employee_id, month, year)
);

-- Late Grace Config
CREATE TABLE late_grace_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID REFERENCES organizations(id),
    max_allowed_per_month INTEGER NOT NULL DEFAULT 4,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Escalation Rules
CREATE TABLE escalation_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    organization_id UUID REFERENCES organizations(id),
    level INTEGER NOT NULL,
    timeout_minutes INTEGER NOT NULL,
    escalate_to_role VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Holidays
CREATE TABLE holidays (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    date DATE NOT NULL,
    year INTEGER NOT NULL,
    is_recurring BOOLEAN DEFAULT false,
    organization_id UUID REFERENCES organizations(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(date, organization_id)
);

-- Anomaly Scores
CREATE TABLE anomaly_scores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    attendance_record_id UUID NOT NULL,
    employee_id UUID NOT NULL REFERENCES employees(id),
    risk_score INTEGER NOT NULL,
    anomaly_type VARCHAR(50),
    description TEXT,
    model_version VARCHAR(50),
    is_escalated BOOLEAN DEFAULT false,
    reviewed_by UUID REFERENCES employees(id),
    review_status VARCHAR(20),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Chatbot Sessions
CREATE TABLE chatbot_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    started_at TIMESTAMPTZ DEFAULT NOW(),
    ended_at TIMESTAMPTZ,
    message_count INTEGER DEFAULT 0,
    escalated_to_hr BOOLEAN DEFAULT false
);

-- Chatbot Messages
CREATE TABLE chatbot_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES chatbot_sessions(id),
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    intent VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Config Changes (Maker-Checker)
CREATE TABLE config_changes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    change_type VARCHAR(20) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    requested_by UUID NOT NULL REFERENCES employees(id),
    requested_at TIMESTAMPTZ DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID REFERENCES employees(id),
    reviewed_at TIMESTAMPTZ,
    review_comment TEXT
);

-- Audit Logs (immutable)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    user_email VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255),
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Report pre-aggregation tables
CREATE TABLE report_attendance_daily (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    date DATE NOT NULL,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    total_employees INTEGER,
    present_count INTEGER,
    absent_count INTEGER,
    late_count INTEGER,
    early_leave_count INTEGER,
    on_leave_count INTEGER,
    suspicious_count INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(date, organization_id)
);

CREATE TABLE report_attendance_weekly (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    week_start DATE NOT NULL,
    week_end DATE NOT NULL,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    avg_attendance_rate DECIMAL(5,2),
    total_late_count INTEGER,
    total_anomaly_count INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(week_start, organization_id)
);

CREATE TABLE report_kpi_daily (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    date DATE NOT NULL,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    attendance_rate DECIMAL(5,2),
    on_time_rate DECIMAL(5,2),
    avg_work_hours DECIMAL(5,2),
    ot_hours DECIMAL(6,2),
    leave_rate DECIMAL(5,2),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(date, organization_id)
);

-- Notification Log
CREATE TABLE notification_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(500),
    body TEXT,
    status VARCHAR(20) DEFAULT 'SENT',
    channel_response TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- QR Codes
CREATE TABLE qr_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    location_id UUID NOT NULL REFERENCES locations(id),
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    is_used BOOLEAN DEFAULT false,
    used_by UUID REFERENCES employees(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_employees_org ON employees(organization_id);
CREATE INDEX idx_employees_email ON employees(email);
CREATE INDEX idx_employees_code ON employees(employee_code);
CREATE INDEX idx_attendance_employee_time ON attendance_records(employee_id, check_time);
CREATE INDEX idx_attendance_location ON attendance_records(location_id, check_time);
CREATE INDEX idx_attendance_status ON attendance_records(status, check_time);
CREATE INDEX idx_timesheets_employee_period ON timesheets(employee_id, period_year, period_month);
CREATE INDEX idx_leave_requests_employee ON leave_requests(employee_id, status);
CREATE INDEX idx_leave_requests_approver ON leave_requests(current_approver_id, status);
CREATE INDEX idx_anomaly_employee ON anomaly_scores(employee_id, created_at);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id, created_at);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource, resource_id, created_at);
CREATE INDEX idx_wifi_bssid ON wifi_access_points(bssid);
CREATE INDEX idx_locations_org ON locations(organization_id);
CREATE INDEX idx_qr_codes_token ON qr_codes(token) WHERE NOT is_used;
CREATE INDEX idx_late_grace_quota_lookup ON late_grace_quota(employee_id, year, month);
CREATE INDEX idx_notification_logs_employee ON notification_logs(employee_id, created_at);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token) WHERE NOT revoked;
