package com.hdbank.attendance.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApproval {
    private UUID id;
    private UUID leaveRequestId;
    private UUID approverId;
    private int level;
    private ApprovalAction action;
    private String comment;
    private String reason;
    private Instant actionAt;

    public enum ApprovalAction {
        APPROVED, REJECTED
    }
}
