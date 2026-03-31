package com.hdbank.auth.domain.service;

public interface TotpService {
    String generateSecret();
    String generateQrUri(String secret, String email);
    boolean verifyCode(String secret, String code);
}
