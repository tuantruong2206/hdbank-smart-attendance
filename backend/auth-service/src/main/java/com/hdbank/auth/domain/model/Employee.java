package com.hdbank.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    private UUID id;
    private String employeeCode;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private UUID organizationId;
    private UUID primaryLocationId;
    private EmployeeType employeeType;
    private Role role;
    private boolean isActive;
    private boolean twoFactorEnabled;
    private TwoFactorMethod twoFactorMethod;
    private String totpSecret;
    private String deviceId;
    private Instant lastLoginAt;

    public enum EmployeeType { NGHIEP_VU, IT_KY_THUAT }
    public enum TwoFactorMethod { TOTP, EMAIL, SMS }
    public enum Role {
        SYSTEM_ADMIN, CEO, DIVISION_DIRECTOR, REGION_DIRECTOR,
        DEPT_HEAD, DEPUTY_HEAD, UNIT_HEAD, EMPLOYEE
    }

    public void enableTwoFactor(TwoFactorMethod method, String secret) {
        this.twoFactorEnabled = true;
        this.twoFactorMethod = method;
        this.totpSecret = secret;
    }

    public void disableTwoFactor() {
        this.twoFactorEnabled = false;
        this.twoFactorMethod = null;
        this.totpSecret = null;
    }

    public boolean isAdminOrAbove() {
        return role == Role.SYSTEM_ADMIN || role == Role.CEO;
    }
}
