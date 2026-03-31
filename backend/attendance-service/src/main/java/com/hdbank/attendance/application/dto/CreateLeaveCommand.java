package com.hdbank.attendance.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateLeaveCommand(
        UUID employeeId,
        String leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {}
