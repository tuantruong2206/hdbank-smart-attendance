package com.hdbank.auth.application.dto;

public record Verify2FACommand(String partialToken, String code, String method) {}
