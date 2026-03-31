CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    resource VARCHAR(50) NOT NULL,
    scope VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    UNIQUE(role, action, resource)
);

-- EMPLOYEE
INSERT INTO permissions (role, action, resource, scope) VALUES
('EMPLOYEE', 'view', 'attendance', 'self'),
('EMPLOYEE', 'create', 'attendance', 'self'),
('EMPLOYEE', 'view', 'timesheet', 'self'),
('EMPLOYEE', 'view', 'leave', 'self'),
('EMPLOYEE', 'create', 'leave', 'self'),
('EMPLOYEE', 'delete', 'leave', 'self'),
('EMPLOYEE', 'view', 'notification', 'self'),
('EMPLOYEE', 'view', 'report', 'self');

-- UNIT_HEAD
INSERT INTO permissions (role, action, resource, scope) VALUES
('UNIT_HEAD', 'view', 'attendance', 'unit'),
('UNIT_HEAD', 'create', 'attendance', 'self'),
('UNIT_HEAD', 'view', 'timesheet', 'unit'),
('UNIT_HEAD', 'approve', 'timesheet', 'unit'),
('UNIT_HEAD', 'view', 'leave', 'unit'),
('UNIT_HEAD', 'create', 'leave', 'self'),
('UNIT_HEAD', 'approve', 'leave', 'unit'),
('UNIT_HEAD', 'view', 'report', 'unit'),
('UNIT_HEAD', 'view', 'notification', 'self');

-- DEPUTY_HEAD
INSERT INTO permissions (role, action, resource, scope) VALUES
('DEPUTY_HEAD', 'view', 'attendance', 'department'),
('DEPUTY_HEAD', 'create', 'attendance', 'self'),
('DEPUTY_HEAD', 'view', 'timesheet', 'department'),
('DEPUTY_HEAD', 'approve', 'timesheet', 'department'),
('DEPUTY_HEAD', 'view', 'leave', 'department'),
('DEPUTY_HEAD', 'create', 'leave', 'self'),
('DEPUTY_HEAD', 'approve', 'leave', 'department'),
('DEPUTY_HEAD', 'view', 'report', 'department'),
('DEPUTY_HEAD', 'export', 'report', 'department'),
('DEPUTY_HEAD', 'view', 'notification', 'self');

-- DEPT_HEAD
INSERT INTO permissions (role, action, resource, scope) VALUES
('DEPT_HEAD', 'view', 'attendance', 'department'),
('DEPT_HEAD', 'create', 'attendance', 'self'),
('DEPT_HEAD', 'view', 'timesheet', 'department'),
('DEPT_HEAD', 'approve', 'timesheet', 'department'),
('DEPT_HEAD', 'view', 'leave', 'department'),
('DEPT_HEAD', 'create', 'leave', 'self'),
('DEPT_HEAD', 'approve', 'leave', 'department'),
('DEPT_HEAD', 'view', 'report', 'department'),
('DEPT_HEAD', 'export', 'report', 'department'),
('DEPT_HEAD', 'view', 'employee', 'department'),
('DEPT_HEAD', 'view', 'notification', 'self');

-- REGION_DIRECTOR
INSERT INTO permissions (role, action, resource, scope) VALUES
('REGION_DIRECTOR', 'view', 'attendance', 'region'),
('REGION_DIRECTOR', 'view', 'timesheet', 'region'),
('REGION_DIRECTOR', 'approve', 'timesheet', 'region'),
('REGION_DIRECTOR', 'view', 'leave', 'region'),
('REGION_DIRECTOR', 'approve', 'leave', 'region'),
('REGION_DIRECTOR', 'view', 'report', 'region'),
('REGION_DIRECTOR', 'export', 'report', 'region'),
('REGION_DIRECTOR', 'view', 'employee', 'region');

-- DIVISION_DIRECTOR
INSERT INTO permissions (role, action, resource, scope) VALUES
('DIVISION_DIRECTOR', 'view', 'attendance', 'all'),
('DIVISION_DIRECTOR', 'view', 'timesheet', 'all'),
('DIVISION_DIRECTOR', 'approve', 'timesheet', 'all'),
('DIVISION_DIRECTOR', 'view', 'leave', 'all'),
('DIVISION_DIRECTOR', 'approve', 'leave', 'all'),
('DIVISION_DIRECTOR', 'view', 'report', 'all'),
('DIVISION_DIRECTOR', 'export', 'report', 'all'),
('DIVISION_DIRECTOR', 'view', 'employee', 'all');

-- CEO
INSERT INTO permissions (role, action, resource, scope) VALUES
('CEO', 'view', 'attendance', 'all'),
('CEO', 'view', 'timesheet', 'all'),
('CEO', 'approve', 'timesheet', 'all'),
('CEO', 'lock', 'timesheet', 'all'),
('CEO', 'view', 'leave', 'all'),
('CEO', 'approve', 'leave', 'all'),
('CEO', 'view', 'report', 'all'),
('CEO', 'export', 'report', 'all'),
('CEO', 'view', 'employee', 'all'),
('CEO', 'view', 'admin', 'all');

-- SYSTEM_ADMIN
INSERT INTO permissions (role, action, resource, scope) VALUES
('SYSTEM_ADMIN', 'view', 'attendance', 'all'),
('SYSTEM_ADMIN', 'create', 'attendance', 'all'),
('SYSTEM_ADMIN', 'edit', 'attendance', 'all'),
('SYSTEM_ADMIN', 'view', 'timesheet', 'all'),
('SYSTEM_ADMIN', 'approve', 'timesheet', 'all'),
('SYSTEM_ADMIN', 'lock', 'timesheet', 'all'),
('SYSTEM_ADMIN', 'view', 'leave', 'all'),
('SYSTEM_ADMIN', 'create', 'leave', 'all'),
('SYSTEM_ADMIN', 'approve', 'leave', 'all'),
('SYSTEM_ADMIN', 'view', 'report', 'all'),
('SYSTEM_ADMIN', 'export', 'report', 'all'),
('SYSTEM_ADMIN', 'view', 'employee', 'all'),
('SYSTEM_ADMIN', 'create', 'employee', 'all'),
('SYSTEM_ADMIN', 'edit', 'employee', 'all'),
('SYSTEM_ADMIN', 'delete', 'employee', 'all'),
('SYSTEM_ADMIN', 'view', 'admin', 'all'),
('SYSTEM_ADMIN', 'create', 'admin', 'all'),
('SYSTEM_ADMIN', 'edit', 'admin', 'all'),
('SYSTEM_ADMIN', 'view', 'organization', 'all'),
('SYSTEM_ADMIN', 'create', 'organization', 'all'),
('SYSTEM_ADMIN', 'edit', 'organization', 'all');
