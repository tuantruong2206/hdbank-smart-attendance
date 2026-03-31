package com.hdbank.attendance.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

public record RejectLeaveRequest(
        String comment,
        @NotBlank String reason
) {}
