package com.hdbank.report.service;

import com.hdbank.report.entity.ReportAttendanceDailyJpaEntity;
import com.hdbank.report.entity.ReportKpiDailyJpaEntity;
import com.hdbank.report.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ReportAttendanceDailyRepository dailyRepository;
    private final ReportKpiDailyRepository kpiDailyRepository;
    private final AttendanceRecordViewRepository attendanceRecordRepository;
    private final EmployeeViewRepository employeeRepository;
    private final OrganizationViewRepository organizationRepository;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Get metrics based on role and organization.
     * Returns different data sets depending on the caller's role.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMetrics(UUID organizationId, String role) {
        LocalDate today = LocalDate.now();

        return switch (role) {
            case "EMPLOYEE" -> getEmployeeMetrics(organizationId, today);
            case "UNIT_HEAD", "DEPT_HEAD" -> getManagerMetrics(organizationId, today);
            case "CEO", "DIRECTOR" -> getExecutiveMetrics(organizationId, today);
            case "SYSTEM_ADMIN" -> getSystemAdminMetrics(today);
            default -> getEmployeeMetrics(organizationId, today);
        };
    }

    private Map<String, Object> getEmployeeMetrics(UUID organizationId, LocalDate today) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("date", today.toString());
        metrics.put("role", "EMPLOYEE");

        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Personal attendance count this month
        long personalAttendanceCount = attendanceRecordRepository.countDistinctCheckIns(monthStart, dayEnd);
        metrics.put("personalAttendanceThisMonth", personalAttendanceCount);

        // Late count this month (would need employee-specific query in production)
        metrics.put("lateCountThisMonth", 0);

        // Leave balance (would come from a leave service in production)
        metrics.put("leaveBalance", 12);

        // Late grace remaining (4 per month default)
        metrics.put("lateGraceRemaining", 4);
        metrics.put("lateGraceTotal", 4);

        return metrics;
    }

    private Map<String, Object> getManagerMetrics(UUID organizationId, LocalDate today) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("date", today.toString());
        metrics.put("role", "MANAGER");

        Optional<ReportAttendanceDailyJpaEntity> dailyOpt = organizationId != null
                ? dailyRepository.findByReportDateAndOrganizationId(today, organizationId)
                : Optional.empty();

        if (dailyOpt.isPresent()) {
            ReportAttendanceDailyJpaEntity daily = dailyOpt.get();
            metrics.put("totalEmployees", daily.getTotalEmployees());
            metrics.put("presentToday", daily.getPresentCount());
            metrics.put("absentToday", daily.getAbsentCount());
            metrics.put("lateToday", daily.getLateCount());
            metrics.put("onLeave", daily.getOnLeaveCount());
            metrics.put("attendanceRate", daily.getAttendanceRate());
            metrics.put("onTimeRate", daily.getOnTimeRate());
            metrics.put("suspiciousRecords", daily.getSuspiciousCount());
        } else {
            // Fallback: compute from live data
            Instant dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant dayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

            long totalEmployees = organizationId != null
                    ? employeeRepository.countActiveByOrganization(organizationId)
                    : employeeRepository.countActive();

            long presentToday = attendanceRecordRepository.countDistinctCheckIns(dayStart, dayEnd);
            long suspicious = attendanceRecordRepository.countSuspiciousCheckIns(dayStart, dayEnd);

            metrics.put("totalEmployees", totalEmployees);
            metrics.put("presentToday", presentToday);
            metrics.put("absentToday", Math.max(0, totalEmployees - presentToday));
            metrics.put("lateToday", 0);
            metrics.put("onLeave", 0);
            metrics.put("attendanceRate", totalEmployees > 0
                    ? Math.round(presentToday * 1000.0 / totalEmployees) / 10.0 : 0.0);
            metrics.put("onTimeRate", 0.0);
            metrics.put("suspiciousRecords", suspicious);
        }

        // Pending approvals (would come from attendance-service in production)
        metrics.put("pendingApprovals", 0);

        return metrics;
    }

    private Map<String, Object> getExecutiveMetrics(UUID organizationId, LocalDate today) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("date", today.toString());
        metrics.put("role", "EXECUTIVE");

        // Org-wide KPIs
        Optional<ReportKpiDailyJpaEntity> kpiOpt = organizationId != null
                ? kpiDailyRepository.findByReportDateAndOrganizationId(today, organizationId)
                : Optional.empty();

        if (kpiOpt.isPresent()) {
            ReportKpiDailyJpaEntity kpi = kpiOpt.get();
            metrics.put("attendanceCompliancePct", kpi.getAttendanceCompliancePct());
            metrics.put("onTimePct", kpi.getOnTimePct());
            metrics.put("fraudIncidentCount", kpi.getFraudIncidentCount());
            metrics.put("escalationCount", kpi.getEscalationCount());
        } else {
            metrics.put("attendanceCompliancePct", 0.0);
            metrics.put("onTimePct", 0.0);
            metrics.put("fraudIncidentCount", 0);
            metrics.put("escalationCount", 0);
        }

        // Branch comparison data
        List<ReportAttendanceDailyJpaEntity> allBranchReports =
                dailyRepository.findAllByDateOrderByRate(today);
        List<Map<String, Object>> branchComparison = new ArrayList<>();
        for (ReportAttendanceDailyJpaEntity branch : allBranchReports) {
            Map<String, Object> branchData = new LinkedHashMap<>();
            branchData.put("organizationId", branch.getOrganizationId());
            branchData.put("organizationName", branch.getOrganizationName());
            branchData.put("attendanceRate", branch.getAttendanceRate());
            branchData.put("lateCount", branch.getLateCount());
            branchData.put("suspiciousCount", branch.getSuspiciousCount());
            branchComparison.add(branchData);
        }
        metrics.put("branchComparison", branchComparison);

        // Anomaly count (would come from ai-service)
        metrics.put("anomalyCount", 0);

        return metrics;
    }

    private Map<String, Object> getSystemAdminMetrics(LocalDate today) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("date", today.toString());
        metrics.put("role", "SYSTEM_ADMIN");

        // System health
        metrics.put("systemStatus", "HEALTHY");
        metrics.put("activeServices", 7);
        metrics.put("totalServices", 7);

        // Total active employees
        long totalEmployees = employeeRepository.countActive();
        metrics.put("totalActiveEmployees", totalEmployees);

        // Today's live stats
        Instant dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        long presentToday = attendanceRecordRepository.countDistinctCheckIns(dayStart, dayEnd);
        long suspicious = attendanceRecordRepository.countSuspiciousCheckIns(dayStart, dayEnd);

        metrics.put("presentToday", presentToday);
        metrics.put("suspiciousToday", suspicious);

        // Pending config changes (would come from admin-service)
        metrics.put("pendingConfigChanges", 0);

        return metrics;
    }

    /**
     * Get team pulse for managers: today's status of each team member.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTeamPulse(UUID organizationId) {
        if (organizationId == null) return List.of();

        LocalDate today = LocalDate.now();
        Instant dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        var employees = employeeRepository.findActiveByOrganization(organizationId);
        List<Map<String, Object>> teamStatus = new ArrayList<>();

        for (var emp : employees) {
            var records = attendanceRecordRepository
                    .findByEmployeeAndTimeRange(emp.getId(), dayStart, dayEnd);

            boolean checkedIn = records.stream()
                    .anyMatch(r -> "CHECK_IN".equals(r.getCheckType()));
            boolean checkedOut = records.stream()
                    .anyMatch(r -> "CHECK_OUT".equals(r.getCheckType()));

            String status;
            if (checkedOut) status = "COMPLETED";
            else if (checkedIn) status = "PRESENT";
            else status = "ABSENT";

            Map<String, Object> empStatus = new LinkedHashMap<>();
            empStatus.put("employeeId", emp.getId());
            empStatus.put("employeeCode", emp.getEmployeeCode());
            empStatus.put("fullName", emp.getFullName());
            empStatus.put("status", status);
            teamStatus.add(empStatus);
        }

        return teamStatus;
    }

    /**
     * Get branch comparison for executives.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBranchComparison(LocalDate date) {
        List<ReportAttendanceDailyJpaEntity> reports = dailyRepository.findAllByDateOrderByRate(date);
        List<Map<String, Object>> comparison = new ArrayList<>();

        for (var report : reports) {
            Map<String, Object> branchData = new LinkedHashMap<>();
            branchData.put("organizationId", report.getOrganizationId());
            branchData.put("organizationName", report.getOrganizationName());
            branchData.put("totalEmployees", report.getTotalEmployees());
            branchData.put("presentCount", report.getPresentCount());
            branchData.put("absentCount", report.getAbsentCount());
            branchData.put("lateCount", report.getLateCount());
            branchData.put("attendanceRate", report.getAttendanceRate());
            branchData.put("onTimeRate", report.getOnTimeRate());
            comparison.add(branchData);
        }

        return comparison;
    }

    public SseEmitter createSseEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    public void broadcastMetrics(Map<String, Object> metrics) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("metrics").data(metrics));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }
}
