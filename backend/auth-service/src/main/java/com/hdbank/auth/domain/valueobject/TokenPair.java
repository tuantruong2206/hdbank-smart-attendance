package com.hdbank.auth.domain.valueobject;

public record TokenPair(String accessToken, String refreshToken, long expiresIn) {}
