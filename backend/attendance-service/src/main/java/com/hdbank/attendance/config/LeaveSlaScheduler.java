package com.hdbank.attendance.config;

import com.hdbank.attendance.application.port.out.EventPublisher;
import com.hdbank.attendance.application.port.out.LeaveRequestRepository;
import com.hdbank.attendance.domain.model.LeaveRequest;
import com.hdbank.common.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Enforces SLA timeouts for leave request approvals.
 *
 * Default SLA per leave type:
 * - SICK: 4 hours (urgent)
 * - ANNUAL: 48 hours (2 business days)
 * - PERSONAL: 24 hours
 *
 * When SLA is breached, sends reminder to current approver
 * and escalates if 2x SLA exceeded.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeaveSlaScheduler {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EventPublisher eventPublisher;

    private static final Map<String, Duration> SLA_BY_TYPE = Map.of(
            "SICK", Duration.ofHours(4),
            "ANNUAL", Duration.ofHours(48),
            "PERSONAL", Duration.ofHours(24),
            "MATERNITY", Duration.ofHours(24)
    );

    private static final Duration DEFAULT_SLA = Duration.ofHours(48);

    /**
     * Check pending leave requests every hour for SLA breaches.
     */
    @Scheduled(fixedRate = 3600_000) // Every 1 hour
    public void checkLeaveSla() {
        List<LeaveRequest> pending = leaveRequestRepository.findAllPending();
        if (pending.isEmpty()) return;

        Instant now = Instant.now();
        int reminders = 0;
        int escalations = 0;

        for (LeaveRequest request : pending) {
            Duration sla = SLA_BY_TYPE.getOrDefault(request.getLeaveType(), DEFAULT_SLA);
            Duration elapsed = Duration.between(request.getUpdatedAt(), now);

            if (elapsed.compareTo(sla.multipliedBy(2)) > 0) {
                // 2x SLA exceeded — escalate
                log.warn("Leave request {} exceeded 2x SLA ({} > {}), escalating",
                        request.getId(), elapsed, sla.multipliedBy(2));

                eventPublisher.publishAudit(Map.of(
                        "type", "LEAVE_SLA_ESCALATION",
                        "requestId", request.getId().toString(),
                        "employeeId", request.getEmployeeId().toString(),
                        "approverId", request.getCurrentApproverId().toString(),
                        "leaveType", request.getLeaveType(),
                        "elapsed", elapsed.toString(),
                        "sla", sla.toString()
                ));
                escalations++;
            } else if (elapsed.compareTo(sla) > 0) {
                // SLA exceeded — send reminder to approver
                log.info("Leave request {} exceeded SLA ({} > {}), sending reminder",
                        request.getId(), elapsed, sla);

                eventPublisher.publishCheckIn(Map.of(
                        "type", "LEAVE_SLA_REMINDER",
                        "targetEmployeeId", request.getCurrentApproverId().toString(),
                        "title", "Nhắc nhở: Đơn nghỉ phép chờ duyệt quá hạn",
                        "body", String.format("Đơn nghỉ phép loại %s đã chờ duyệt %d giờ (SLA: %d giờ). Vui lòng xử lý.",
                                request.getLeaveType(), elapsed.toHours(), sla.toHours()),
                        "priority", "HIGH"
                ));
                reminders++;
            }
        }

        if (reminders > 0 || escalations > 0) {
            log.info("Leave SLA check: {} reminders sent, {} escalations", reminders, escalations);
        }
    }
}
