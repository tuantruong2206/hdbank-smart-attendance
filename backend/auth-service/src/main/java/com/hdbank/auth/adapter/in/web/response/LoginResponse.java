package com.hdbank.auth.adapter.in.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hdbank.auth.domain.valueobject.TokenPair;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        boolean requires2FA,
        String partialToken,
        Object employeeInfo
) {
    public static LoginResponse from(TokenPair tokenPair) {
        return new LoginResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.expiresIn(),
                false, null, null
        );
    }
}
