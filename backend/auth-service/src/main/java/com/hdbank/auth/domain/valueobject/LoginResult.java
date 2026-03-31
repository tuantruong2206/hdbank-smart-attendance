package com.hdbank.auth.domain.valueobject;

import java.util.UUID;

public record LoginResult(
        TokenPair tokenPair,
        boolean requires2FA,
        String partialToken,
        UUID employeeId
) {}
