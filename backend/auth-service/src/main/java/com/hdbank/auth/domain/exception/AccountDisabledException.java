package com.hdbank.auth.domain.exception;

public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() {
        super("Account is disabled");
    }
}
