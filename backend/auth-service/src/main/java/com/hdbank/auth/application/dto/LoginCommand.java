package com.hdbank.auth.application.dto;

public record LoginCommand(String email, String password, String deviceId, String ipAddress) {}
