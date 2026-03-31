package com.hdbank.auth.adapter.in.web;

import com.hdbank.auth.adapter.in.web.request.LoginRequest;
import com.hdbank.auth.adapter.in.web.request.Verify2FARequest;
import com.hdbank.auth.adapter.in.web.response.EmployeeInfoResponse;
import com.hdbank.auth.adapter.in.web.response.LoginResponse;
import com.hdbank.auth.application.dto.LoginCommand;
import com.hdbank.auth.application.dto.Setup2FAResult;
import com.hdbank.auth.application.dto.Verify2FACommand;
import com.hdbank.auth.application.port.in.LoginUseCase;
import com.hdbank.auth.application.port.in.RefreshTokenUseCase;
import com.hdbank.auth.application.port.in.RequestOtpUseCase;
import com.hdbank.auth.application.port.in.SetupTwoFactorUseCase;
import com.hdbank.auth.application.port.in.VerifyTwoFactorUseCase;
import com.hdbank.auth.application.port.out.EmployeeAuthRepository;
import com.hdbank.auth.domain.model.Employee;
import com.hdbank.auth.domain.valueobject.LoginResult;
import com.hdbank.auth.domain.valueobject.TokenPair;
import com.hdbank.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final VerifyTwoFactorUseCase verify2faUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final SetupTwoFactorUseCase setup2faUseCase;
    private final RequestOtpUseCase requestOtpUseCase;
    private final EmployeeAuthRepository employeeRepo;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResult result = loginUseCase.login(new LoginCommand(
                request.email(), request.password(),
                request.deviceId(), httpRequest.getRemoteAddr()
        ));

        LoginResponse response;
        if (result.requires2FA()) {
            response = new LoginResponse(null, null, 0, true, result.partialToken(), null);
        } else {
            Employee emp = employeeRepo.findById(result.employeeId()).orElse(null);
            EmployeeInfoResponse info = emp != null ? EmployeeInfoResponse.from(emp) : null;
            response = new LoginResponse(
                    result.tokenPair().accessToken(),
                    result.tokenPair().refreshToken(),
                    result.tokenPair().expiresIn(),
                    false, null, info);
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<ApiResponse<LoginResponse>> verify2fa(
            @Valid @RequestBody Verify2FARequest request) {
        TokenPair tokenPair = verify2faUseCase.verify(
                new Verify2FACommand(request.partialToken(), request.code(), request.method()));
        return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(tokenPair)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestBody Map<String, String> body) {
        TokenPair tokenPair = refreshTokenUseCase.refresh(body.get("refreshToken"));
        return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(tokenPair)));
    }

    @PostMapping("/setup-2fa")
    public ResponseEntity<ApiResponse<Setup2FAResult>> setup2fa(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {
        String method = body.getOrDefault("method", "TOTP");
        Setup2FAResult result = setup2faUseCase.setup(UUID.fromString(userId), method);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/disable-2fa")
    public ResponseEntity<ApiResponse<Void>> disable2fa(
            @RequestHeader("X-User-Id") String userId) {
        setup2faUseCase.disable(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success("2FA disabled", null));
    }

    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestOtp(
            @RequestBody Map<String, String> body) {
        UUID employeeId = UUID.fromString(body.get("employeeId"));
        String method = body.getOrDefault("method", "EMAIL");
        requestOtpUseCase.requestOtp(employeeId, method);
        return ResponseEntity.ok(ApiResponse.success("OTP sent", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<EmployeeInfoResponse>> getCurrentUser(
            @RequestHeader("X-User-Id") String userId) {
        return employeeRepo.findById(UUID.fromString(userId))
                .map(emp -> ResponseEntity.ok(ApiResponse.success(EmployeeInfoResponse.from(emp))))
                .orElse(ResponseEntity.notFound().build());
    }
}
