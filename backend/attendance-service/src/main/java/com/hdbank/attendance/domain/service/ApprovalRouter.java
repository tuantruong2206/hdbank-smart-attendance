package com.hdbank.attendance.domain.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Pure domain logic for routing leave approvals up the org tree.
 * No Spring dependencies — injected via constructor functions (ports).
 */
public class ApprovalRouter {

    /**
     * Represents a node in the organizational hierarchy.
     */
    public record OrgNode(UUID managerId, UUID parentOrgId, UUID deputyManagerId) {}

    /**
     * Represents an absence record for rerouting logic.
     */
    public record AbsenceInfo(UUID employeeId, Instant absentSince) {}

    private final Function<UUID, Optional<OrgNode>> orgNodeLookup;
    private final Function<UUID, Optional<AbsenceInfo>> absenceLookup;

    public ApprovalRouter(
            Function<UUID, Optional<OrgNode>> orgNodeLookup,
            Function<UUID, Optional<AbsenceInfo>> absenceLookup) {
        this.orgNodeLookup = orgNodeLookup;
        this.absenceLookup = absenceLookup;
    }

    /**
     * Find the next approver by walking up the org tree from the current approver's org unit.
     */
    public Optional<UUID> findNextApprover(UUID currentApproverId, List<UUID> organizationPath) {
        if (organizationPath == null || organizationPath.isEmpty()) {
            return Optional.empty();
        }

        int currentIndex = -1;
        for (int i = 0; i < organizationPath.size(); i++) {
            Optional<OrgNode> node = orgNodeLookup.apply(organizationPath.get(i));
            if (node.isPresent() && node.get().managerId().equals(currentApproverId)) {
                currentIndex = i;
                break;
            }
        }

        // Walk up from current position
        for (int i = currentIndex + 1; i < organizationPath.size(); i++) {
            Optional<OrgNode> parentNode = orgNodeLookup.apply(organizationPath.get(i));
            if (parentNode.isPresent()) {
                UUID nextManager = parentNode.get().managerId();
                if (nextManager != null && !nextManager.equals(currentApproverId)) {
                    return Optional.of(nextManager);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Check if approver has been absent for more than 3 days.
     */
    public boolean shouldAutoReroute(UUID approverId) {
        return absenceLookup.apply(approverId)
                .map(absence -> {
                    long daysSinceAbsent = ChronoUnit.DAYS.between(absence.absentSince(), Instant.now());
                    return daysSinceAbsent > 3;
                })
                .orElse(false);
    }

    /**
     * Find a substitute approver: try deputy first, then next level up.
     */
    public Optional<UUID> findSubstituteApprover(UUID approverId, List<UUID> organizationPath) {
        // Try to find the org node of this approver
        if (organizationPath != null) {
            for (UUID orgId : organizationPath) {
                Optional<OrgNode> node = orgNodeLookup.apply(orgId);
                if (node.isPresent() && node.get().managerId().equals(approverId)) {
                    // Try deputy first
                    if (node.get().deputyManagerId() != null) {
                        return Optional.of(node.get().deputyManagerId());
                    }
                    break;
                }
            }
        }

        // Fall back to next level up
        return findNextApprover(approverId, organizationPath);
    }
}
