package com.hdbank.auth.application.service;

import com.hdbank.auth.application.dto.LoginCommand;
import com.hdbank.auth.application.dto.Verify2FACommand;
import com.hdbank.auth.application.port.in.LoginUseCase;
import com.hdbank.auth.application.port.in.RefreshTokenUseCase;
import com.hdbank.auth.application.port.in.VerifyTwoFactorUseCase;
import com.hdbank.auth.application.port.out.*;
import com.hdbank.auth.domain.exception.AccountDisabledException;
import com.hdbank.auth.domain.exception.InvalidCredentialsException;
import com.hdbank.auth.domain.exception.InvalidOtpException;
import com.hdbank.auth.domain.model.Employee;
import com.hdbank.auth.domain.model.RefreshToken;
import com.hdbank.auth.domain.service.PasswordHasher;
import com.hdbank.auth.domain.service.TotpService;
import com.hdbank.auth.domain.valueobject.LoginResult;
import com.hdbank.auth.domain.valueobject.TokenPair;
import com.hdbank.common.event.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService implements LoginUseCase, VerifyTwoFactorUseCase, RefreshTokenUseCase {

    private final EmployeeAuthRepository employeeRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final TokenProvider tokenProvider;
    private final PasswordHasher passwordHasher;
    private final TotpService totpService;
    private final EventPublisher eventPublisher;
    private final OtpCachePort otpCache;

    @Override
    @Transactional
    public LoginResult login(LoginCommand command) {
        Employee employee = employeeRepo.findByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!employee.isActive()) {
            throw new AccountDisabledException();
        }

        if (!passwordHasher.matches(command.password(), employee.getPasswordHash())) {
            eventPublisher.publish("attendance.audit", AuditEvent.builder()
                    .userEmail(command.email())
                    .action("LOGIN_FAILED")
                    .resource("auth")
                    .ipAddress(command.ipAddress())
                    .build());
            throw new InvalidCredentialsException();
        }

        // Device attestation: detect device change and log audit event
        if (command.deviceId() != null && employee.getDeviceId() != null
                && !command.deviceId().equals(employee.getDeviceId())) {
            eventPublisher.publish("attendance.audit", AuditEvent.builder()
                    .userId(employee.getId())
                    .userEmail(employee.getEmail())
                    .action("DEVICE_CHANGED")
                    .resource("auth")
                    .oldValue(employee.getDeviceId())
                    .newValue(command.deviceId())
                    .ipAddress(command.ipAddress())
                    .build());
        }

        if (employee.isTwoFactorEnabled()) {
            String partialToken = tokenProvider.generatePartialToken(employee);
            return new LoginResult(null, true, partialToken, employee.getId());
        }

        TokenPair tokenPair = generateTokens(employee);
        employee.setLastLoginAt(Instant.now());
        employee.setDeviceId(command.deviceId());
        employeeRepo.save(employee);

        eventPublisher.publish("attendance.audit", AuditEvent.builder()
                .userId(employee.getId())
                .userEmail(employee.getEmail())
                .action("LOGIN_SUCCESS")
                .resource("auth")
                .ipAddress(command.ipAddress())
                .build());

        return new LoginResult(tokenPair, false, null, employee.getId());
    }

    @Override
    @Transactional
    public TokenPair verify(Verify2FACommand command) {
        UUID employeeId = tokenProvider.getEmployeeIdFromToken(command.partialToken());
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(InvalidCredentialsException::new);

        boolean valid = switch (command.method()) {
            case "TOTP" -> totpService.verifyCode(employee.getTotpSecret(), command.code());
            case "EMAIL", "SMS" -> {
                var stored = otpCache.getOtp("otp:" + employeeId);
                yield stored.isPresent() && stored.get().equals(command.code());
            }
            default -> false;
        };

        if (!valid) {
            throw new InvalidOtpException();
        }

        otpCache.deleteOtp("otp:" + employeeId);
        TokenPair tokenPair = generateTokens(employee);
        employee.setLastLoginAt(Instant.now());
        employeeRepo.save(employee);

        return tokenPair;
    }

    @Override
    @Transactional
    public TokenPair refresh(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepo.findByToken(refreshTokenStr)
                .orElseThrow(InvalidCredentialsException::new);

        if (refreshToken.isExpired() || refreshToken.isRevoked()) {
            throw new InvalidCredentialsException();
        }

        Employee employee = employeeRepo.findById(refreshToken.getEmployeeId())
                .orElseThrow(InvalidCredentialsException::new);

        refreshToken.revoke();
        refreshTokenRepo.save(refreshToken);

        return generateTokens(employee);
    }

    private TokenPair generateTokens(Employee employee) {
        String accessToken = tokenProvider.generateAccessToken(employee);
        String refreshTokenStr = tokenProvider.generateRefreshToken(employee);

        RefreshToken refreshToken = RefreshToken.builder()
                .employeeId(employee.getId())
                .token(refreshTokenStr)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .build();
        refreshTokenRepo.save(refreshToken);

        return new TokenPair(accessToken, refreshTokenStr, 900);
    }
}
