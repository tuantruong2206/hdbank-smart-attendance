package com.hdbank.auth.application.port.in;

import com.hdbank.auth.application.dto.Setup2FAResult;

import java.util.UUID;

public interface SetupTwoFactorUseCase {
    Setup2FAResult setup(UUID employeeId, String method);
    void disable(UUID employeeId);
}
