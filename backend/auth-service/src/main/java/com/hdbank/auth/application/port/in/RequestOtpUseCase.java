package com.hdbank.auth.application.port.in;

import java.util.UUID;

public interface RequestOtpUseCase {
    void requestOtp(UUID employeeId, String method);
}
