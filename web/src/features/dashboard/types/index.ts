import type { Role } from '@/shared/types';

// --- Employee Widgets ---
export interface TodayStatusData {
  checkInTime: string | null;
  checkOutTime: string | null;
  status: 'PRESENT' | 'ABSENT' | 'LATE' | 'ON_LEAVE' | 'NOT_CHECKED_IN';
}

export interface PersonalAttendanceCountData {
  workedDays: number;
  totalWorkDays: number;
}

export interface LateCountData {
  lateCount: number;
  month: number;
}

export interface LeaveBalanceData {
  annual: number;
  sick: number;
  personal: number;
  annualTotal: number;
  sickTotal: number;
  personalTotal: number;
}

export interface LateGraceQuotaData {
  used: number;
  total: number;
}

export interface CurrentShiftData {
  shiftName: string;
  startTime: string;
  endTime: string;
}

export interface MonthlyOTHoursData {
  otHours: number;
}

export interface HolidayItem {
  name: string;
  date: string;
}

export interface PendingLeaveRequestsData {
  count: number;
}

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  createdAt: string;
  read: boolean;
}

export interface WeeklyDayStatus {
  day: string;
  status: 'PRESENT' | 'ABSENT' | 'LATE' | 'LEAVE' | 'WEEKEND' | 'NOT_YET';
}

// --- Manager Widgets ---
export interface TeamAttendanceRateData {
  rate: number;
  presentCount: number;
  totalCount: number;
}

export interface TeamMemberStatus {
  employeeCode: string;
  fullName: string;
  status: 'PRESENT' | 'ABSENT' | 'LATE' | 'ON_LEAVE';
  checkInTime: string | null;
}

export interface PendingApprovalsCountData {
  leaveApprovals: number;
  timesheetApprovals: number;
  total: number;
}

export interface LateEmployeeItem {
  employeeCode: string;
  fullName: string;
  minutesLate: number;
}

export interface AbsentEmployeeItem {
  employeeCode: string;
  fullName: string;
  department: string;
}

export interface LeaveCalendarItem {
  employeeCode: string;
  fullName: string;
  dates: string[];
  leaveType: string;
}

export interface BranchKPITrendPoint {
  date: string;
  rate: number;
}

export interface TeamOTSummaryData {
  totalHours: number;
  employeeCount: number;
}

export interface SuspiciousRecordsData {
  count: number;
  weekStart: string;
}

// --- Executive Widgets ---
export interface OrgAttendanceRateData {
  rate: number;
}

export interface BranchComparisonItem {
  branchName: string;
  rate: number;
}

export interface AttendanceTrendPoint {
  date: string;
  rate: number;
}

export interface AnomalySummaryData {
  buddyPunching: number;
  location: number;
  time: number;
  device: number;
  total: number;
}

export interface LeaveRateData {
  rate: number;
}

export interface EscalationSummaryData {
  level1: number;
  level2: number;
  level3: number;
  total: number;
}

export interface WorkforceDistributionItem {
  type: string;
  count: number;
}

// --- System Admin Widgets ---
export interface ServiceStatusItem {
  name: string;
  status: 'UP' | 'DOWN';
  url: string;
}

export interface PendingConfigChangesData {
  count: number;
}

export interface AuditLogEntry {
  id: string;
  action: string;
  performedBy: string;
  target: string;
  timestamp: string;
}

// Role group helpers
export const EMPLOYEE_ROLES: Role[] = ['EMPLOYEE'];
export const MANAGER_ROLES: Role[] = ['DEPT_HEAD', 'DEPUTY_HEAD', 'UNIT_HEAD'];
export const EXECUTIVE_ROLES: Role[] = ['CEO', 'DIVISION_DIRECTOR', 'REGION_DIRECTOR'];
export const ADMIN_ROLES: Role[] = ['SYSTEM_ADMIN'];

export function isEmployee(role: string): boolean {
  return role === 'EMPLOYEE';
}

export function isManager(role: string): boolean {
  return MANAGER_ROLES.includes(role as Role);
}

export function isExecutive(role: string): boolean {
  return EXECUTIVE_ROLES.includes(role as Role);
}

export function isAdmin(role: string): boolean {
  return role === 'SYSTEM_ADMIN';
}
