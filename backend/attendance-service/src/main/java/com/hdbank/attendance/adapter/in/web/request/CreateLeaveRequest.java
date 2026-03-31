package com.hdbank.attendance.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateLeaveRequest(
        @NotBlank String leaveType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason
) {}
