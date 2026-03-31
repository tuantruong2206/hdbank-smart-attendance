import api from '@/shared/api/axiosInstance';
import type {
  TodayStatusData,
  PersonalAttendanceCountData,
  LateCountData,
  LeaveBalanceData,
  LateGraceQuotaData,
  CurrentShiftData,
  MonthlyOTHoursData,
  HolidayItem,
  PendingLeaveRequestsData,
  NotificationItem,
  WeeklyDayStatus,
  TeamAttendanceRateData,
  TeamMemberStatus,
  PendingApprovalsCountData,
  LateEmployeeItem,
  AbsentEmployeeItem,
  LeaveCalendarItem,
  BranchKPITrendPoint,
  TeamOTSummaryData,
  SuspiciousRecordsData,
  OrgAttendanceRateData,
  BranchComparisonItem,
  AttendanceTrendPoint,
  AnomalySummaryData,
  LeaveRateData,
  EscalationSummaryData,
  WorkforceDistributionItem,
  ServiceStatusItem,
  PendingConfigChangesData,
  AuditLogEntry,
} from '../types';

const extract = <T>(res: { data: { data: T } }) => res.data.data;

// --- Employee ---
export const fetchTodayStatus = () =>
  api.get<{ data: TodayStatusData }>('/dashboard/widgets/today-status').then(extract);

export const fetchPersonalAttendanceCount = () =>
  api.get<{ data: PersonalAttendanceCountData }>('/dashboard/widgets/personal-attendance-count').then(extract);

export const fetchLateCount = () =>
  api.get<{ data: LateCountData }>('/dashboard/widgets/late-count').then(extract);

export const fetchLeaveBalance = () =>
  api.get<{ data: LeaveBalanceData }>('/dashboard/widgets/leave-balance').then(extract);

export const fetchLateGraceQuota = () =>
  api.get<{ data: LateGraceQuotaData }>('/dashboard/widgets/late-grace-quota').then(extract);

export const fetchCurrentShift = () =>
  api.get<{ data: CurrentShiftData }>('/dashboard/widgets/current-shift').then(extract);

export const fetchMonthlyOTHours = () =>
  api.get<{ data: MonthlyOTHoursData }>('/dashboard/widgets/monthly-ot-hours').then(extract);

export const fetchUpcomingHolidays = () =>
  api.get<{ data: HolidayItem[] }>('/dashboard/widgets/upcoming-holidays').then(extract);

export const fetchPendingLeaveRequests = () =>
  api.get<{ data: PendingLeaveRequestsData }>('/dashboard/widgets/pending-leave-requests').then(extract);

export const fetchRecentNotifications = () =>
  api.get<{ data: NotificationItem[] }>('/dashboard/widgets/recent-notifications').then(extract);

export const fetchWeeklyAttendanceSummary = () =>
  api.get<{ data: WeeklyDayStatus[] }>('/dashboard/widgets/weekly-attendance-summary').then(extract);

// --- Manager ---
export const fetchTeamAttendanceRate = () =>
  api.get<{ data: TeamAttendanceRateData }>('/dashboard/widgets/team-attendance-rate').then(extract);

export const fetchTeamPulse = () =>
  api.get<{ data: TeamMemberStatus[] }>('/dashboard/widgets/team-pulse').then(extract);

export const fetchPendingApprovalsCount = () =>
  api.get<{ data: PendingApprovalsCountData }>('/dashboard/widgets/pending-approvals-count').then(extract);

export const fetchLateEmployeesToday = () =>
  api.get<{ data: LateEmployeeItem[] }>('/dashboard/widgets/late-employees-today').then(extract);

export const fetchAbsentList = () =>
  api.get<{ data: AbsentEmployeeItem[] }>('/dashboard/widgets/absent-list').then(extract);

export const fetchLeaveCalendar = () =>
  api.get<{ data: LeaveCalendarItem[] }>('/dashboard/widgets/leave-calendar').then(extract);

export const fetchBranchKPITrend = () =>
  api.get<{ data: BranchKPITrendPoint[] }>('/dashboard/widgets/branch-kpi-trend').then(extract);

export const fetchTeamOTSummary = () =>
  api.get<{ data: TeamOTSummaryData }>('/dashboard/widgets/team-ot-summary').then(extract);

export const fetchSuspiciousRecords = () =>
  api.get<{ data: SuspiciousRecordsData }>('/dashboard/widgets/suspicious-records').then(extract);

// --- Executive ---
export const fetchOrgAttendanceRate = () =>
  api.get<{ data: OrgAttendanceRateData }>('/dashboard/widgets/org-attendance-rate').then(extract);

export const fetchBranchComparison = () =>
  api.get<{ data: BranchComparisonItem[] }>('/dashboard/widgets/branch-comparison').then(extract);

export const fetchAttendanceTrend = () =>
  api.get<{ data: AttendanceTrendPoint[] }>('/dashboard/widgets/attendance-trend').then(extract);

export const fetchAnomalySummary = () =>
  api.get<{ data: AnomalySummaryData }>('/dashboard/widgets/anomaly-summary').then(extract);

export const fetchLeaveRate = () =>
  api.get<{ data: LeaveRateData }>('/dashboard/widgets/leave-rate').then(extract);

export const fetchEscalationSummary = () =>
  api.get<{ data: EscalationSummaryData }>('/dashboard/widgets/escalation-summary').then(extract);

export const fetchWorkforceDistribution = () =>
  api.get<{ data: WorkforceDistributionItem[] }>('/dashboard/widgets/workforce-distribution').then(extract);

// --- System Admin ---
export const fetchSystemHealth = () =>
  api.get<{ data: ServiceStatusItem[] }>('/dashboard/widgets/system-health').then(extract);

export const fetchPendingConfigChanges = () =>
  api.get<{ data: PendingConfigChangesData }>('/dashboard/widgets/pending-config-changes').then(extract);

export const fetchRecentAuditLog = () =>
  api.get<{ data: AuditLogEntry[] }>('/dashboard/widgets/recent-audit-log').then(extract);
