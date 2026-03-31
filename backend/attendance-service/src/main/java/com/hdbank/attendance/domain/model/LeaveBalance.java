package com.hdbank.attendance.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance {
    private UUID id;
    private UUID employeeId;
    private int year;
    private String leaveType;
    private int totalDays;
    private int usedDays;
    private int pendingDays;

    public int getAvailableDays() {
        return totalDays - usedDays - pendingDays;
    }

    public void deduct(int days) {
        if (days > getAvailableDays()) {
            throw new IllegalStateException(
                    "Không đủ ngày phép. Còn lại: " + getAvailableDays() + ", yêu cầu: " + days);
        }
        this.pendingDays += days;
    }

    public void confirmUsed(int days) {
        this.pendingDays -= days;
        this.usedDays += days;
    }

    public void releasePending(int days) {
        this.pendingDays -= days;
    }
}
