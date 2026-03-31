package com.hdbank.report.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeView {

    @Id
    private UUID id;

    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(nullable = false)
    private String role;

    @Column(name = "is_active")
    private boolean isActive;
}
