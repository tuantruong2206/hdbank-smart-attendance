import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/shared/hooks/useAuth';
import { isEmployee, isManager, isExecutive, isAdmin } from '../types';
import * as dashboardApi from '../api/dashboardApi';

const REFETCH_INTERVAL = 60_000; // 1 minute

function useWidget<T>(
  key: string,
  fn: () => Promise<T>,
  enabled: boolean,
  refetchInterval = REFETCH_INTERVAL,
) {
  return useQuery<T>({
    queryKey: ['dashboard', key],
    queryFn: fn,
    enabled,
    refetchInterval,
    staleTime: 30_000,
  });
}

export function useDashboardData() {
  const { user } = useAuth();
  const role = user?.role || '';

  const showEmployee = isEmployee(role) || isManager(role);
  const showManager = isManager(role);
  const showExecutive = isExecutive(role);
  const showAdmin = isAdmin(role);

  // Employee widgets
  const todayStatus = useWidget('today-status', dashboardApi.fetchTodayStatus, showEmployee);
  const personalAttendance = useWidget('personal-attendance', dashboardApi.fetchPersonalAttendanceCount, showEmployee);
  const lateCount = useWidget('late-count', dashboardApi.fetchLateCount, showEmployee);
  const leaveBalance = useWidget('leave-balance', dashboardApi.fetchLeaveBalance, showEmployee);
  const lateGraceQuota = useWidget('late-grace-quota', dashboardApi.fetchLateGraceQuota, showEmployee);
  const currentShift = useWidget('current-shift', dashboardApi.fetchCurrentShift, showEmployee);
  const monthlyOT = useWidget('monthly-ot', dashboardApi.fetchMonthlyOTHours, showEmployee);
  const upcomingHolidays = useWidget('upcoming-holidays', dashboardApi.fetchUpcomingHolidays, showEmployee);
  const pendingLeaveReqs = useWidget('pending-leave-reqs', dashboardApi.fetchPendingLeaveRequests, showEmployee);
  const recentNotifications = useWidget('recent-notifications', dashboardApi.fetchRecentNotifications, showEmployee);
  const weeklySummary = useWidget('weekly-summary', dashboardApi.fetchWeeklyAttendanceSummary, showEmployee);

  // Manager widgets
  const teamAttendanceRate = useWidget('team-attendance-rate', dashboardApi.fetchTeamAttendanceRate, showManager);
  const teamPulse = useWidget('team-pulse', dashboardApi.fetchTeamPulse, showManager);
  const pendingApprovals = useWidget('pending-approvals', dashboardApi.fetchPendingApprovalsCount, showManager);
  const lateEmployees = useWidget('late-employees', dashboardApi.fetchLateEmployeesToday, showManager);
  const absentList = useWidget('absent-list', dashboardApi.fetchAbsentList, showManager);
  const leaveCalendar = useWidget('leave-calendar', dashboardApi.fetchLeaveCalendar, showManager);
  const branchKPITrend = useWidget('branch-kpi-trend', dashboardApi.fetchBranchKPITrend, showManager);
  const teamOTSummary = useWidget('team-ot-summary', dashboardApi.fetchTeamOTSummary, showManager);
  const suspiciousRecords = useWidget('suspicious-records', dashboardApi.fetchSuspiciousRecords, showManager);

  // Executive widgets
  const orgAttendanceRate = useWidget('org-attendance-rate', dashboardApi.fetchOrgAttendanceRate, showExecutive);
  const branchComparison = useWidget('branch-comparison', dashboardApi.fetchBranchComparison, showExecutive);
  const attendanceTrend = useWidget('attendance-trend', dashboardApi.fetchAttendanceTrend, showExecutive);
  const anomalySummary = useWidget('anomaly-summary', dashboardApi.fetchAnomalySummary, showExecutive);
  const leaveRate = useWidget('leave-rate', dashboardApi.fetchLeaveRate, showExecutive);
  const escalationSummary = useWidget('escalation-summary', dashboardApi.fetchEscalationSummary, showExecutive);
  const workforceDistribution = useWidget('workforce-distribution', dashboardApi.fetchWorkforceDistribution, showExecutive);

  // Admin widgets
  const systemHealth = useWidget('system-health', dashboardApi.fetchSystemHealth, showAdmin, 15_000);
  const pendingConfigChanges = useWidget('pending-config-changes', dashboardApi.fetchPendingConfigChanges, showAdmin);
  const recentAuditLog = useWidget('recent-audit-log', dashboardApi.fetchRecentAuditLog, showAdmin);

  return {
    role,
    showEmployee,
    showManager,
    showExecutive,
    showAdmin,
    // Employee
    todayStatus,
    personalAttendance,
    lateCount,
    leaveBalance,
    lateGraceQuota,
    currentShift,
    monthlyOT,
    upcomingHolidays,
    pendingLeaveReqs,
    recentNotifications,
    weeklySummary,
    // Manager
    teamAttendanceRate,
    teamPulse,
    pendingApprovals,
    lateEmployees,
    absentList,
    leaveCalendar,
    branchKPITrend,
    teamOTSummary,
    suspiciousRecords,
    // Executive
    orgAttendanceRate,
    branchComparison,
    attendanceTrend,
    anomalySummary,
    leaveRate,
    escalationSummary,
    workforceDistribution,
    // Admin
    systemHealth,
    pendingConfigChanges,
    recentAuditLog,
  };
}
