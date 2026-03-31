import { Typography, Spin, Row, Col, Divider } from 'antd';
import { useAuth } from '@/shared/hooks/useAuth';
import { useDashboardData } from '@/features/dashboard/hooks/useDashboardData';
import {
  // Employee (1-11)
  TodayStatus,
  PersonalAttendanceCount,
  LateCount,
  LeaveBalance,
  LateGraceQuota,
  CurrentShift,
  MonthlyOTHours,
  UpcomingHolidays,
  PendingLeaveRequests,
  RecentNotifications,
  WeeklyAttendanceSummary,
  // Manager (12-20)
  TeamAttendanceRate,
  TeamPulseTable,
  PendingApprovalsCount,
  LateEmployeesToday,
  AbsentList,
  LeaveCalendar,
  BranchKPITrend,
  TeamOTSummary,
  SuspiciousRecords,
  // Executive (21-27)
  OrgAttendanceRate,
  BranchComparison,
  AttendanceTrend,
  AnomalySummary,
  LeaveRate,
  EscalationSummary,
  WorkforceDistribution,
  // System Admin (28-30)
  SystemHealth,
  PendingConfigChanges,
  RecentAuditLog,
} from '@/features/dashboard/components/widgets';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export default function DashboardPage() {
  const { user } = useAuth();
  const dashboard = useDashboardData();

  if (!user) {
    return <div style={{ textAlign: 'center', padding: 50 }}><Spin size="large" /></div>;
  }

  return (
    <div>
      <Title level={4}>Tong quan — {dayjs().format('DD/MM/YYYY')}</Title>
      <Text type="secondary">Xin chao, {user.fullName}! Vai tro: {user.role}</Text>

      {/* === EMPLOYEE WIDGETS === */}
      {dashboard.showEmployee && (
        <>
          <Divider orientation="left">Thong tin ca nhan</Divider>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} md={8} lg={6}>
              <TodayStatus query={dashboard.todayStatus} />
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <PersonalAttendanceCount query={dashboard.personalAttendance} />
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <LateCount query={dashboard.lateCount} />
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <LateGraceQuota query={dashboard.lateGraceQuota} />
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <CurrentShift query={dashboard.currentShift} />
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <MonthlyOTHours query={dashboard.monthlyOT} />
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <PendingLeaveRequests query={dashboard.pendingLeaveReqs} />
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <LeaveBalance query={dashboard.leaveBalance} />
            </Col>
          </Row>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} md={12}>
              <WeeklyAttendanceSummary query={dashboard.weeklySummary} />
            </Col>
            <Col xs={24} md={12}>
              <UpcomingHolidays query={dashboard.upcomingHolidays} />
            </Col>
          </Row>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24}>
              <RecentNotifications query={dashboard.recentNotifications} />
            </Col>
          </Row>
        </>
      )}

      {/* === MANAGER WIDGETS === */}
      {dashboard.showManager && (
        <>
          <Divider orientation="left">Quan ly nhom</Divider>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} md={8} lg={6}>
              <TeamAttendanceRate query={dashboard.teamAttendanceRate} />
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <PendingApprovalsCount query={dashboard.pendingApprovals} />
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <TeamOTSummary query={dashboard.teamOTSummary} />
            </Col>
            <Col xs={24} sm={12} md={8} lg={6}>
              <SuspiciousRecords query={dashboard.suspiciousRecords} />
            </Col>
          </Row>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} lg={12}>
              <TeamPulseTable query={dashboard.teamPulse} />
            </Col>
            <Col xs={24} lg={12}>
              <LateEmployeesToday query={dashboard.lateEmployees} />
            </Col>
          </Row>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} lg={12}>
              <AbsentList query={dashboard.absentList} />
            </Col>
            <Col xs={24} lg={12}>
              <LeaveCalendar query={dashboard.leaveCalendar} />
            </Col>
          </Row>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24}>
              <BranchKPITrend query={dashboard.branchKPITrend} />
            </Col>
          </Row>
        </>
      )}

      {/* === EXECUTIVE WIDGETS === */}
      {dashboard.showExecutive && (
        <>
          <Divider orientation="left">Tong quan dieu hanh</Divider>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} md={8}>
              <OrgAttendanceRate query={dashboard.orgAttendanceRate} />
            </Col>
            <Col xs={24} sm={12} md={8}>
              <LeaveRate query={dashboard.leaveRate} />
            </Col>
            <Col xs={24} sm={12} md={8}>
              <EscalationSummary query={dashboard.escalationSummary} />
            </Col>
          </Row>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} sm={12} md={8}>
              <AnomalySummary query={dashboard.anomalySummary} />
            </Col>
            <Col xs={24} sm={12} md={16}>
              <WorkforceDistribution query={dashboard.workforceDistribution} />
            </Col>
          </Row>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} lg={12}>
              <BranchComparison query={dashboard.branchComparison} />
            </Col>
            <Col xs={24} lg={12}>
              <AttendanceTrend query={dashboard.attendanceTrend} />
            </Col>
          </Row>
        </>
      )}

      {/* === SYSTEM ADMIN WIDGETS === */}
      {dashboard.showAdmin && (
        <>
          <Divider orientation="left">Quan tri he thong</Divider>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} md={8}>
              <SystemHealth query={dashboard.systemHealth} />
            </Col>
            <Col xs={24} sm={12} md={8}>
              <PendingConfigChanges query={dashboard.pendingConfigChanges} />
            </Col>
          </Row>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24}>
              <RecentAuditLog query={dashboard.recentAuditLog} />
            </Col>
          </Row>
        </>
      )}
    </div>
  );
}
