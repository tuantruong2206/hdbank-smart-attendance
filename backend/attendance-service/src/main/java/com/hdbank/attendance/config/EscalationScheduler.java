package com.hdbank.attendance.config;

import com.hdbank.attendance.adapter.out.persistence.entity.EmployeeJpaEntity;
import com.hdbank.attendance.adapter.out.persistence.entity.EscalationTrackingJpaEntity;
import com.hdbank.attendance.adapter.out.persistence.entity.ShiftJpaEntity;
import com.hdbank.attendance.adapter.out.persistence.repository.*;
import com.hdbank.attendance.application.port.out.EscalationRuleRepository;
import com.hdbank.attendance.application.port.out.EscalationRuleRepository.EscalationRuleRecord;
import com.hdbank.attendance.application.port.out.EventPublisher;
import com.hdbank.attendance.domain.service.EscalationEngine;
import com.hdbank.common.event.EscalationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Scheduled job that checks for absent/late employees and triggers escalation.
 * Runs every 30 minutes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EscalationScheduler {

    private final EmployeeAttendanceJpaRepository employeeRepository;
    private final AttendanceRecordJpaRepository attendanceRepository;
    private final ShiftJpaRepository shiftRepository;
    private final EscalationRuleRepository escalationRuleRepository;
    private final EscalationTrackingJpaRepository escalationTrackingRepository;
    private final EscalationEngine escalationEngine;
    private final EventPublisher eventPublisher;

    private static final LocalTime DEFAULT_SHIFT_START = LocalTime.of(8, 0);
    private static final int DEFAULT_GRACE_MINUTES = 15;

    @Scheduled(fixedRate = 30 * 60 * 1000) // Every 30 minutes
    @Transactional
    public void checkAndEscalate() {
        log.info("Escalation check started");
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<EmployeeJpaEntity> activeEmployees = employeeRepository.findAllActiveEmployees();
        log.debug("Checking {} active employees for escalation", activeEmployees.size());

        for (EmployeeJpaEntity employee : activeEmployees) {
            try {
                processEmployee(employee, today, now);
            } catch (Exception e) {
                log.error("Error processing escalation for employee {}: {}",
                        employee.getEmployeeCode(), e.getMessage(), e);
            }
        }

        // Also re-evaluate existing pending escalations for level-up
        reEvaluatePendingEscalations(today);

        log.info("Escalation check completed");
    }

    private void processEmployee(EmployeeJpaEntity employee, LocalDate today, LocalTime now) {
        // Determine the shift for this employee
        ShiftJpaEntity shift = resolveShift(employee);
        LocalTime shiftStart = shift != null ? shift.getStartTime() : DEFAULT_SHIFT_START;
        int gracePeriod = shift != null ? shift.getGracePeriodMinutes() : DEFAULT_GRACE_MINUTES;

        LocalTime deadlineTime = shiftStart.plusMinutes(gracePeriod);

        // Only check if we are past the shift start + grace period
        if (now.isBefore(deadlineTime)) {
            return;
        }

        // Check if employee has a check-in for today
        Instant dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        var todayRecords = attendanceRepository
                .findByEmployeeAndTimeRange(employee.getId(), dayStart, dayEnd);

        boolean hasCheckedIn = todayRecords.stream()
                .anyMatch(r -> "CHECK_IN".equals(r.getCheckType()));

        boolean isLate = false;
        if (hasCheckedIn) {
            isLate = todayRecords.stream()
                    .filter(r -> "CHECK_IN".equals(r.getCheckType()))
                    .anyMatch(r -> {
                        LocalTime checkTime = r.getCheckTime()
                                .atZone(ZoneId.systemDefault()).toLocalTime();
                        return checkTime.isAfter(shiftStart.plusMinutes(gracePeriod));
                    });
        }

        boolean isSuspicious = todayRecords.stream()
                .anyMatch(r -> "SUSPICIOUS".equals(r.getStatus()));

        String triggerType = escalationEngine.determineTriggerType(hasCheckedIn, isLate, isSuspicious);
        if (triggerType == null) {
            return; // No escalation needed
        }

        // Check if we already have an escalation tracking entry for today
        Optional<EscalationTrackingJpaEntity> existingTracking = escalationTrackingRepository
                .findByEmployeeIdAndTriggerDateAndTriggerType(employee.getId(), today, triggerType);

        if (existingTracking.isPresent()) {
            // Already being tracked, level-up handled in reEvaluatePendingEscalations
            return;
        }

        // Get escalation rules for this org
        List<EscalationRuleRecord> rules = escalationRuleRepository
                .findByOrganizationAndTriggerType(employee.getOrganizationId(), triggerType);

        if (rules.isEmpty()) {
            log.debug("No escalation rules found for org {} trigger {}",
                    employee.getOrganizationId(), triggerType);
            return;
        }

        // Create level 1 escalation
        EscalationRuleRecord level1Rule = rules.stream()
                .filter(r -> r.level() == 1)
                .findFirst()
                .orElse(rules.get(0));

        // Find escalation target
        List<EmployeeJpaEntity> targets = employeeRepository
                .findByOrganizationIdAndRole(employee.getOrganizationId(), level1Rule.targetRole());

        EmployeeJpaEntity target = targets.isEmpty() ? null : targets.get(0);

        // Create tracking record
        EscalationTrackingJpaEntity tracking = EscalationTrackingJpaEntity.builder()
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .organizationId(employee.getOrganizationId())
                .triggerType(triggerType)
                .triggerDate(today)
                .currentLevel(1)
                .escalatedToId(target != null ? target.getId() : null)
                .escalatedToCode(target != null ? target.getEmployeeCode() : null)
                .status("PENDING")
                .lastEscalationTime(Instant.now())
                .build();

        escalationTrackingRepository.save(tracking);

        // Publish escalation event
        publishEscalationEvent(employee, tracking, target, level1Rule);

        log.info("Level 1 escalation created for employee {} ({}): {}",
                employee.getEmployeeCode(), triggerType,
                target != null ? target.getEmployeeCode() : "no target");
    }

    private void reEvaluatePendingEscalations(LocalDate today) {
        List<EscalationTrackingJpaEntity> pending = escalationTrackingRepository.findActiveByDate(today);

        for (EscalationTrackingJpaEntity tracking : pending) {
            try {
                List<EscalationRuleRecord> rules = escalationRuleRepository
                        .findByOrganizationAndTriggerType(tracking.getOrganizationId(), tracking.getTriggerType());

                long minutesSinceCreation = Duration.between(tracking.getCreatedAt(), Instant.now()).toMinutes();
                int newLevel = escalationEngine.determineEscalationLevel(
                        rules.stream().map(r -> new EscalationEngine.EscalationRule(
                                r.id(), r.organizationId(), r.triggerType(),
                                r.level(), r.targetRole(), r.timeoutMinutes(), r.isActive()
                        )).toList(),
                        minutesSinceCreation
                );

                if (newLevel > tracking.getCurrentLevel()) {
                    // Level up
                    Optional<EscalationRuleRecord> newRule = rules.stream()
                            .filter(r -> r.level() == newLevel)
                            .findFirst();

                    if (newRule.isPresent()) {
                        List<EmployeeJpaEntity> targets = employeeRepository
                                .findByOrganizationIdAndRole(
                                        tracking.getOrganizationId(), newRule.get().targetRole());

                        EmployeeJpaEntity target = targets.isEmpty() ? null : targets.get(0);

                        tracking.setCurrentLevel(newLevel);
                        tracking.setEscalatedToId(target != null ? target.getId() : null);
                        tracking.setEscalatedToCode(target != null ? target.getEmployeeCode() : null);
                        tracking.setLastEscalationTime(Instant.now());
                        escalationTrackingRepository.save(tracking);

                        EmployeeJpaEntity employee = employeeRepository.findById(tracking.getEmployeeId())
                                .orElse(null);

                        if (employee != null) {
                            publishEscalationEvent(employee, tracking, target, newRule.get());
                        }

                        log.info("Escalation leveled up to {} for employee {} ({})",
                                newLevel, tracking.getEmployeeCode(), tracking.getTriggerType());
                    }
                }
            } catch (Exception e) {
                log.error("Error re-evaluating escalation {}: {}", tracking.getId(), e.getMessage(), e);
            }
        }
    }

    private ShiftJpaEntity resolveShift(EmployeeJpaEntity employee) {
        // Look up shift via employee_shifts table or fall back to org-level default
        return shiftRepository.findByOrganizationId(employee.getOrganizationId()).orElse(null);
    }

    private void publishEscalationEvent(
            EmployeeJpaEntity employee,
            EscalationTrackingJpaEntity tracking,
            EmployeeJpaEntity target,
            EscalationRuleRecord rule) {

        String reason = switch (tracking.getTriggerType()) {
            case "ABSENT" -> "Nhân viên " + employee.getFullName()
                    + " (" + employee.getEmployeeCode() + ") chưa chấm công hôm nay";
            case "LATE" -> "Nhân viên " + employee.getFullName()
                    + " (" + employee.getEmployeeCode() + ") đi trễ hôm nay";
            case "SUSPICIOUS" -> "Phát hiện chấm công bất thường cho nhân viên "
                    + employee.getFullName() + " (" + employee.getEmployeeCode() + ")";
            default -> "Escalation for " + employee.getEmployeeCode();
        };

        EscalationEvent event = EscalationEvent.builder()
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .organizationId(employee.getOrganizationId())
                .escalationLevel(tracking.getCurrentLevel())
                .triggerType(tracking.getTriggerType())
                .reason(reason)
                .escalatedTo(target != null ? target.getId() : null)
                .escalatedToCode(target != null ? target.getEmployeeCode() : null)
                .escalatedToRole(rule.targetRole())
                .timeoutMinutes(rule.timeoutMinutes())
                .status("PENDING")
                .build();

        eventPublisher.publishEscalation(event);
    }
}
