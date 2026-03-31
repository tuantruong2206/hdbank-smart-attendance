package com.hdbank.attendance.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest {
    private UUID id;
    private UUID employeeId;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalDays;
    private String reason;
    private LeaveStatus status;
    private UUID currentApproverId;
    private int currentApprovalLevel;
    private int maxApprovalLevel;
    @Builder.Default
    private List<LeaveApproval> approvals = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public enum LeaveStatus {
        PENDING, APPROVED, REJECTED, CANCELLED
    }

    public void approve(UUID approverId, String comment) {
        if (this.status != LeaveStatus.PENDING) {
            throw new IllegalStateException(
                    "Chỉ có thể duyệt đơn ở trạng thái PENDING. Trạng thái hiện tại: " + this.status);
        }
        LeaveApproval approval = LeaveApproval.builder()
                .id(UUID.randomUUID())
                .leaveRequestId(this.id)
                .approverId(approverId)
                .level(this.currentApprovalLevel)
                .action(LeaveApproval.ApprovalAction.APPROVED)
                .comment(comment)
                .actionAt(Instant.now())
                .build();
        this.approvals.add(approval);

        if (this.currentApprovalLevel >= this.maxApprovalLevel) {
            this.status = LeaveStatus.APPROVED;
        } else {
            this.currentApprovalLevel++;
        }
        this.updatedAt = Instant.now();
    }

    public void reject(UUID approverId, String comment, String reason) {
        if (this.status != LeaveStatus.PENDING) {
            throw new IllegalStateException(
                    "Chỉ có thể từ chối đơn ở trạng thái PENDING. Trạng thái hiện tại: " + this.status);
        }
        LeaveApproval approval = LeaveApproval.builder()
                .id(UUID.randomUUID())
                .leaveRequestId(this.id)
                .approverId(approverId)
                .level(this.currentApprovalLevel)
                .action(LeaveApproval.ApprovalAction.REJECTED)
                .comment(comment)
                .reason(reason)
                .actionAt(Instant.now())
                .build();
        this.approvals.add(approval);
        this.status = LeaveStatus.REJECTED;
        this.updatedAt = Instant.now();
    }

    public void cancel(UUID employeeId) {
        if (this.status != LeaveStatus.PENDING) {
            throw new IllegalStateException(
                    "Chỉ có thể hủy đơn ở trạng thái PENDING. Trạng thái hiện tại: " + this.status);
        }
        if (!this.employeeId.equals(employeeId)) {
            throw new IllegalArgumentException("Chỉ nhân viên tạo đơn mới có thể hủy đơn nghỉ phép");
        }
        this.status = LeaveStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
