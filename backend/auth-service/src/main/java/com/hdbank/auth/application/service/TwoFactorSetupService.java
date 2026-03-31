package com.hdbank.auth.application.service;

import com.hdbank.auth.application.dto.Setup2FAResult;
import com.hdbank.auth.application.port.in.RequestOtpUseCase;
import com.hdbank.auth.application.port.in.SetupTwoFactorUseCase;
import com.hdbank.auth.application.port.out.EmployeeAuthRepository;
import com.hdbank.auth.application.port.out.OtpCachePort;
import com.hdbank.auth.application.port.out.OtpDeliveryPort;
import com.hdbank.auth.domain.exception.InvalidOtpException;
import com.hdbank.auth.domain.model.Employee;
import com.hdbank.auth.domain.service.TotpService;
import com.hdbank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorSetupService implements SetupTwoFactorUseCase, RequestOtpUseCase {

    private final EmployeeAuthRepository employeeRepo;
    private final TotpService totpService;
    private final OtpCachePort otpCache;
    private final OtpDeliveryPort otpDelivery;
    private static final int OTP_LENGTH = 6;
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int MAX_OTP_ATTEMPTS = 3;

    @Override
    @Transactional
    public Setup2FAResult setup(UUID employeeId, String method) {
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId.toString()));

        if ("TOTP".equals(method)) {
            String secret = totpService.generateSecret();
            String qrUri = totpService.generateQrUri(secret, employee.getEmail());
            employee.enableTwoFactor(Employee.TwoFactorMethod.TOTP, secret);
            employeeRepo.save(employee);
            return new Setup2FAResult(secret, qrUri);
        } else {
            // EMAIL or SMS — just enable, OTP sent on each login
            Employee.TwoFactorMethod tfMethod = Employee.TwoFactorMethod.valueOf(method);
            employee.enableTwoFactor(tfMethod, null);
            employeeRepo.save(employee);
            return new Setup2FAResult(null, null);
        }
    }

    @Override
    @Transactional
    public void disable(UUID employeeId) {
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId.toString()));
        employee.disableTwoFactor();
        employeeRepo.save(employee);
    }

    @Override
    public void requestOtp(UUID employeeId, String method) {
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId.toString()));

        String attemptsKey = "otp-attempts:" + employeeId;
        int attempts = otpCache.incrementAttempts(attemptsKey);
        if (attempts > MAX_OTP_ATTEMPTS) {
            throw new InvalidOtpException();
        }

        String otp = generateOtp();
        otpCache.storeOtp("otp:" + employeeId, otp, OTP_TTL);

        String destination = "SMS".equals(method) ? employee.getPhone() : employee.getEmail();
        otpDelivery.sendOtp(destination, otp, method);
        log.info("OTP sent to employee {} via {}", employeeId, method);
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = random.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }
}
