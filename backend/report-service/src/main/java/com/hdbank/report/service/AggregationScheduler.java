package com.hdbank.report.service;

import com.hdbank.report.entity.*;
import com.hdbank.report.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;

/**
 * Nightly/weekly aggregation scheduler for pre-aggregated report tables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AggregationScheduler {

    private final ReportAttendanceDailyRepository dailyRepository;
    private final ReportAttendanceWeeklyRepository weeklyRepository;
    private final ReportKpiDailyRepository kpiDailyRepository;
    private final AttendanceRecordViewRepository attendanceRecordRepository;
    private final OrganizationViewRepository organizationRepository;
    private final EmployeeViewRepository employeeRepository;

    /**
     * Nightly at 01:00: recalculate report_attendance_daily for yesterday.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void recalculateDailyReport() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Recalculating daily attendance report for {}", yesterday);

        Instant dayStart = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<AttendanceRecordView> records = attendanceRecordRepository.findByTimeRange(dayStart, dayEnd);
        List<OrganizationView> orgs = organizationRepository.findAllActive();

        for (OrganizationView org : orgs) {
            try {
                recalculateDailyForOrg(yesterday, org, records, dayStart, dayEnd);
            } catch (Exception e) {
                log.error("Error recalculating daily for org {}: {}", org.getCode(), e.getMessage(), e);
            }
        }

        log.info("Daily attendance report recalculation completed for {}", yesterday);
    }

    private void recalculateDailyForOrg(LocalDate date, OrganizationView org,
                                         List<AttendanceRecordView> allRecords,
                                         Instant dayStart, Instant dayEnd) {
        long totalEmployees = employeeRepository.countActiveByOrganization(org.getId());
        if (totalEmployees == 0) return;

        // Note: In production, records would be filtered by org via employee->org mapping.
        // For now, we compute system-wide if org is the root, or skip non-root orgs without data.
        long presentCount = allRecords.stream()
                .filter(r -> "CHECK_IN".equals(r.getCheckType()))
                .map(AttendanceRecordView::getEmployeeId)
                .distinct()
                .count();

        long suspiciousCount = allRecords.stream()
                .filter(r -> "SUSPICIOUS".equals(r.getStatus()))
                .map(AttendanceRecordView::getEmployeeId)
                .distinct()
                .count();

        long lateCount = 0; // Would need shift data to determine

        ReportAttendanceDailyJpaEntity daily = dailyRepository
                .findByReportDateAndOrganizationId(date, org.getId())
                .orElseGet(() -> ReportAttendanceDailyJpaEntity.builder()
                        .reportDate(date)
                        .organizationId(org.getId())
                        .organizationName(org.getName())
                        .build());

        daily.setTotalEmployees((int) totalEmployees);
        daily.setPresentCount((int) presentCount);
        daily.setAbsentCount((int) (totalEmployees - presentCount));
        daily.setLateCount((int) lateCount);
        daily.setSuspiciousCount((int) suspiciousCount);

        if (totalEmployees > 0) {
            daily.setAttendanceRate((double) presentCount / totalEmployees * 100);
            daily.setOnTimeRate((double) (presentCount - lateCount) / totalEmployees * 100);
        }

        dailyRepository.save(daily);
    }

    /**
     * Weekly on Monday at 02:00: calculate report_attendance_weekly for previous week.
     */
    @Scheduled(cron = "0 0 2 * * MON")
    @Transactional
    public void calculateWeeklyReport() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        log.info("Calculating weekly attendance report for {} to {}", weekStart, weekEnd);

        List<OrganizationView> orgs = organizationRepository.findAllActive();

        for (OrganizationView org : orgs) {
            try {
                calculateWeeklyForOrg(weekStart, weekEnd, org);
            } catch (Exception e) {
                log.error("Error calculating weekly for org {}: {}", org.getCode(), e.getMessage(), e);
            }
        }

        log.info("Weekly attendance report calculation completed");
    }

    private void calculateWeeklyForOrg(LocalDate weekStart, LocalDate weekEnd, OrganizationView org) {
        List<ReportAttendanceDailyJpaEntity> dailyReports =
                dailyRepository.findByOrgAndDateRange(org.getId(), weekStart, weekEnd);

        if (dailyReports.isEmpty()) return;

        double avgAttendanceRate = dailyReports.stream()
                .mapToDouble(ReportAttendanceDailyJpaEntity::getAttendanceRate)
                .average().orElse(0.0);

        double avgOnTimeRate = dailyReports.stream()
                .mapToDouble(ReportAttendanceDailyJpaEntity::getOnTimeRate)
                .average().orElse(0.0);

        int totalLate = dailyReports.stream()
                .mapToInt(ReportAttendanceDailyJpaEntity::getLateCount).sum();

        int totalAbsent = dailyReports.stream()
                .mapToInt(ReportAttendanceDailyJpaEntity::getAbsentCount).sum();

        int totalLeave = dailyReports.stream()
                .mapToInt(ReportAttendanceDailyJpaEntity::getOnLeaveCount).sum();

        int totalSuspicious = dailyReports.stream()
                .mapToInt(ReportAttendanceDailyJpaEntity::getSuspiciousCount).sum();

        ReportAttendanceWeeklyJpaEntity weekly = weeklyRepository
                .findByWeekStartAndOrganizationId(weekStart, org.getId())
                .orElseGet(() -> ReportAttendanceWeeklyJpaEntity.builder()
                        .weekStart(weekStart)
                        .weekEnd(weekEnd)
                        .organizationId(org.getId())
                        .organizationName(org.getName())
                        .build());

        weekly.setAvgAttendanceRate(avgAttendanceRate);
        weekly.setAvgOnTimeRate(avgOnTimeRate);
        weekly.setTotalLateCount(totalLate);
        weekly.setTotalAbsentCount(totalAbsent);
        weekly.setTotalLeaveCount(totalLeave);
        weekly.setTotalSuspiciousCount(totalSuspicious);

        weeklyRepository.save(weekly);
    }

    /**
     * Daily at 01:30: calculate report_kpi_daily for yesterday.
     */
    @Scheduled(cron = "0 30 1 * * *")
    @Transactional
    public void calculateKpiDaily() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Calculating daily KPI for {}", yesterday);

        Instant dayStart = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<OrganizationView> orgs = organizationRepository.findAllActive();

        for (OrganizationView org : orgs) {
            try {
                ReportAttendanceDailyJpaEntity daily = dailyRepository
                        .findByReportDateAndOrganizationId(yesterday, org.getId())
                        .orElse(null);

                if (daily == null) continue;

                ReportKpiDailyJpaEntity kpi = kpiDailyRepository
                        .findByReportDateAndOrganizationId(yesterday, org.getId())
                        .orElseGet(() -> ReportKpiDailyJpaEntity.builder()
                                .reportDate(yesterday)
                                .organizationId(org.getId())
                                .build());

                kpi.setAttendanceCompliancePct(daily.getAttendanceRate());
                kpi.setOnTimePct(daily.getOnTimeRate());
                kpi.setFraudIncidentCount(daily.getSuspiciousCount());

                kpiDailyRepository.save(kpi);
            } catch (Exception e) {
                log.error("Error calculating KPI for org {}: {}", org.getCode(), e.getMessage(), e);
            }
        }

        log.info("Daily KPI calculation completed for {}", yesterday);
    }
}
