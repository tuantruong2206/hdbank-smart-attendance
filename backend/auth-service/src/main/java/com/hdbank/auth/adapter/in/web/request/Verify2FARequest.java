package com.hdbank.auth.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

public record Verify2FARequest(
        @NotBlank String partialToken,
        @NotBlank String code,
        @NotBlank String method
) {}
