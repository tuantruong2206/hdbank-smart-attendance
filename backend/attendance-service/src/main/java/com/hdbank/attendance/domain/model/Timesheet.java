package com.hdbank.attendance.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Timesheet {
    private UUID id;
    private UUID employeeId;
    private int month;
    private int year;
    private TimesheetStatus status;
    private int totalWorkDays;
    private int lateCount;
    private int earlyLeaveCount;
    private int absentDays;
    @Builder.Default
    private BigDecimal otHours = BigDecimal.ZERO;
    private int lateGraceUsed;
    private UUID approvedBy;
    private Instant approvedAt;
    private UUID lockedBy;
    private Instant lockedAt;
    private Map<String, Object> snapshot;
    private Instant createdAt;
    private Instant updatedAt;

    public enum TimesheetStatus {
        DRAFT, PENDING_REVIEW, APPROVED, LOCKED
    }

    public void submitForReview() {
        if (this.status != TimesheetStatus.DRAFT) {
            throw new IllegalStateException(
                    "Chỉ có thể gửi duyệt từ trạng thái DRAFT. Trạng thái hiện tại: " + this.status);
        }
        this.status = TimesheetStatus.PENDING_REVIEW;
        this.updatedAt = Instant.now();
    }

    public void approve(UUID approverId) {
        if (this.status != TimesheetStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "Chỉ có thể duyệt từ trạng thái PENDING_REVIEW. Trạng thái hiện tại: " + this.status);
        }
        this.status = TimesheetStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void lock(UUID lockerId, Map<String, Object> snapshotData) {
        if (this.status != TimesheetStatus.APPROVED) {
            throw new IllegalStateException(
                    "Chỉ có thể khóa từ trạng thái APPROVED. Trạng thái hiện tại: " + this.status);
        }
        this.status = TimesheetStatus.LOCKED;
        this.lockedBy = lockerId;
        this.lockedAt = Instant.now();
        this.snapshot = snapshotData;
        this.updatedAt = Instant.now();
    }

    public void rejectReview(UUID reviewerId) {
        if (this.status != TimesheetStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "Chỉ có thể từ chối từ trạng thái PENDING_REVIEW. Trạng thái hiện tại: " + this.status);
        }
        this.status = TimesheetStatus.DRAFT;
        this.updatedAt = Instant.now();
    }
}
