package com.hdbank.auth.adapter.in.web.response;

import com.hdbank.auth.domain.model.Employee;

import java.util.UUID;

public record EmployeeInfoResponse(
        UUID id,
        String employeeCode,
        String email,
        String fullName,
        String role,
        String employeeType,
        UUID organizationId,
        boolean twoFactorEnabled,
        String twoFactorMethod
) {
    public static EmployeeInfoResponse from(Employee emp) {
        return new EmployeeInfoResponse(
                emp.getId(),
                emp.getEmployeeCode(),
                emp.getEmail(),
                emp.getFullName(),
                emp.getRole().name(),
                emp.getEmployeeType().name(),
                emp.getOrganizationId(),
                emp.isTwoFactorEnabled(),
                emp.getTwoFactorMethod() != null ? emp.getTwoFactorMethod().name() : null
        );
    }
}
