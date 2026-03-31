package com.hdbank.auth.application.port.in;

import com.hdbank.auth.domain.valueobject.TokenPair;

public interface RefreshTokenUseCase {
    TokenPair refresh(String refreshToken);
}
