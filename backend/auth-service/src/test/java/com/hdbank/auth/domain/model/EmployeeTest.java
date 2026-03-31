package com.hdbank.auth.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeTest {

    @Test
    void enableTwoFactor_sets_fields() {
        var emp = Employee.builder().build();
        emp.enableTwoFactor(Employee.TwoFactorMethod.TOTP, "secret123");
        assertTrue(emp.isTwoFactorEnabled());
        assertEquals(Employee.TwoFactorMethod.TOTP, emp.getTwoFactorMethod());
        assertEquals("secret123", emp.getTotpSecret());
    }

    @Test
    void disableTwoFactor_clears_fields() {
        var emp = Employee.builder()
                .twoFactorEnabled(true)
                .twoFactorMethod(Employee.TwoFactorMethod.TOTP)
                .totpSecret("secret")
                .build();
        emp.disableTwoFactor();
        assertFalse(emp.isTwoFactorEnabled());
        assertNull(emp.getTwoFactorMethod());
        assertNull(emp.getTotpSecret());
    }

    @Test
    void isAdminOrAbove_true_for_system_admin() {
        var emp = Employee.builder().role(Employee.Role.SYSTEM_ADMIN).build();
        assertTrue(emp.isAdminOrAbove());
    }

    @Test
    void isAdminOrAbove_true_for_ceo() {
        var emp = Employee.builder().role(Employee.Role.CEO).build();
        assertTrue(emp.isAdminOrAbove());
    }

    @Test
    void isAdminOrAbove_false_for_dept_head() {
        var emp = Employee.builder().role(Employee.Role.DEPT_HEAD).build();
        assertFalse(emp.isAdminOrAbove());
    }

    @Test
    void isAdminOrAbove_false_for_employee() {
        var emp = Employee.builder().role(Employee.Role.EMPLOYEE).build();
        assertFalse(emp.isAdminOrAbove());
    }
}
