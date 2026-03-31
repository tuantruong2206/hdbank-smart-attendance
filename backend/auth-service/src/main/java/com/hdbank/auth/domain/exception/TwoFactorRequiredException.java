package com.hdbank.auth.domain.exception;

import lombok.Getter;

@Getter
public class TwoFactorRequiredException extends RuntimeException {
    private final String partialToken;

    public TwoFactorRequiredException(String partialToken) {
        super("Two-factor authentication required");
        this.partialToken = partialToken;
    }
}
