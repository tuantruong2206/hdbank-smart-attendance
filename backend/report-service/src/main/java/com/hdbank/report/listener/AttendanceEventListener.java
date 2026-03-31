package com.hdbank.report.listener;

import com.hdbank.report.entity.ReportAttendanceDailyJpaEntity;
import com.hdbank.report.repository.ReportAttendanceDailyRepository;
import com.hdbank.report.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttendanceEventListener {

    private final DashboardService dashboardService;
    private final ReportAttendanceDailyRepository dailyRepository;

    @KafkaListener(topics = "attendance.checkin", groupId = "report-service")
    @Transactional
    public void handleCheckIn(Map<String, Object> event) {
        String employeeCode = getString(event, "employeeCode");
        String checkType = getString(event, "checkType");
        String status = getString(event, "status");
        int fraudScore = getInt(event, "fraudScore");

        log.info("Report service received check-in event: employee={}, type={}, status={}",
                employeeCode, checkType, status);

        try {
            // Determine the date and organization for this event
            LocalDate eventDate = LocalDate.now();
            Object timestampObj = event.get("timestamp");
            if (timestampObj instanceof Number n) {
                eventDate = Instant.ofEpochMilli(n.longValue())
                        .atZone(ZoneId.systemDefault()).toLocalDate();
            }

            // Get organization ID from event or use a default placeholder
            UUID orgId = parseUuid(event.get("organizationId"));
            if (orgId == null) {
                // Use location-based org lookup in production; for now use a system-wide aggregate
                orgId = UUID.fromString("00000000-0000-0000-0000-000000000000");
            }

            // Update daily aggregation
            updateDailyReport(eventDate, orgId, checkType, status, fraudScore);
        } catch (Exception e) {
            log.error("Error updating daily report from check-in event: {}", e.getMessage(), e);
        }

        // Broadcast real-time update via SSE
        try {
            dashboardService.broadcastMetrics(dashboardService.getMetrics(null, "SYSTEM_ADMIN"));
        } catch (Exception e) {
            log.debug("SSE broadcast failed (no active emitters): {}", e.getMessage());
        }
    }

    private void updateDailyReport(LocalDate date, UUID orgId, String checkType,
                                    String status, int fraudScore) {
        ReportAttendanceDailyJpaEntity daily = dailyRepository
                .findByReportDateAndOrganizationId(date, orgId)
                .orElseGet(() -> ReportAttendanceDailyJpaEntity.builder()
                        .reportDate(date)
                        .organizationId(orgId)
                        .build());

        if ("CHECK_IN".equals(checkType)) {
            daily.setPresentCount(daily.getPresentCount() + 1);

            if ("SUSPICIOUS".equals(status) || fraudScore >= 70) {
                daily.setSuspiciousCount(daily.getSuspiciousCount() + 1);
            }
        }

        // Recalculate rates
        if (daily.getTotalEmployees() > 0) {
            daily.setAttendanceRate(
                    (double) daily.getPresentCount() / daily.getTotalEmployees() * 100);
            int onTimeCount = daily.getPresentCount() - daily.getLateCount();
            daily.setOnTimeRate(
                    (double) Math.max(0, onTimeCount) / daily.getTotalEmployees() * 100);
        }

        dailyRepository.save(daily);
    }

    private String getString(Map<String, Object> event, String key) {
        Object val = event.get(key);
        return val != null ? val.toString() : null;
    }

    private int getInt(Map<String, Object> event, String key) {
        Object val = event.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private UUID parseUuid(Object value) {
        if (value == null) return null;
        if (value instanceof UUID u) return u;
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
