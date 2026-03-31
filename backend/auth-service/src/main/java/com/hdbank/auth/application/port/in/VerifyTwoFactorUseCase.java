package com.hdbank.auth.application.port.in;

import com.hdbank.auth.application.dto.Verify2FACommand;
import com.hdbank.auth.domain.valueobject.TokenPair;

public interface VerifyTwoFactorUseCase {
    TokenPair verify(Verify2FACommand command);
}
