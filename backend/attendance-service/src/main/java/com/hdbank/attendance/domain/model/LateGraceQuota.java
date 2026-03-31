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
public class LateGraceQuota {
    private UUID id;
    private UUID employeeId;
    private int month;
    private int year;
    private int maxAllowed;
    private int usedCount;

    public boolean hasRemaining() {
        return usedCount < maxAllowed;
    }

    public void use() {
        if (!hasRemaining()) {
            throw new IllegalStateException("Late grace quota exhausted");
        }
        this.usedCount++;
    }

    public int getRemainingCount() {
        return maxAllowed - usedCount;
    }
}
