package com.hdbank.attendance.domain.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure domain service for escalation logic.
 * No Spring dependencies — only plain Java.
 */
public class EscalationEngine {

    public record EscalationRule(
            UUID id,
            UUID organizationId,
            String triggerType, // ABSENT, LATE, SUSPICIOUS
            int level,
            String targetRole, // UNIT_HEAD, DEPT_HEAD, DIRECTOR
            int timeoutMinutes,
            boolean isActive
    ) {}

    public record EscalationTarget(
            UUID employeeId,
            String employeeCode,
            String role,
            int level
    ) {}

    /**
     * Determine the current escalation level for a given trigger,
     * based on how many minutes have passed since the trigger event.
     */
    public int determineEscalationLevel(List<EscalationRule> rules, long minutesSinceTrigger) {
        int currentLevel = 0;
        long cumulativeTimeout = 0;

        // Rules should be sorted by level ascending
        List<EscalationRule> sortedRules = rules.stream()
                .filter(EscalationRule::isActive)
                .sorted((a, b) -> Integer.compare(a.level(), b.level()))
                .toList();

        for (EscalationRule rule : sortedRules) {
            cumulativeTimeout += rule.timeoutMinutes();
            if (minutesSinceTrigger >= cumulativeTimeout) {
                currentLevel = rule.level();
            } else {
                break;
            }
        }
        return currentLevel;
    }

    /**
     * Find the appropriate escalation target by walking up the org tree.
     * organizationPath is like "/HO/KHOI_CNTT/PHONG_DEV/BP_BACKEND"
     */
    public Optional<String> findTargetRole(int level) {
        return switch (level) {
            case 1 -> Optional.of("UNIT_HEAD");
            case 2 -> Optional.of("DEPT_HEAD");
            case 3 -> Optional.of("DIRECTOR");
            default -> Optional.empty();
        };
    }

    /**
     * Determine whether an escalation should be triggered based on
     * the last escalation time and the configured timeout.
     */
    public boolean shouldEscalate(Instant lastEscalationTime, int timeoutMinutes) {
        if (lastEscalationTime == null) {
            return true;
        }
        Duration elapsed = Duration.between(lastEscalationTime, Instant.now());
        return elapsed.toMinutes() >= timeoutMinutes;
    }

    /**
     * Determine the trigger type based on attendance status.
     * Returns ABSENT if no check-in found, LATE if checked in late.
     */
    public String determineTriggerType(boolean hasCheckedIn, boolean isLate, boolean isSuspicious) {
        if (isSuspicious) return "SUSPICIOUS";
        if (!hasCheckedIn) return "ABSENT";
        if (isLate) return "LATE";
        return null; // No escalation needed
    }
}
