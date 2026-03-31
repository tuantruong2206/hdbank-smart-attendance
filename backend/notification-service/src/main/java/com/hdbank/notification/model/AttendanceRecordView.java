package com.hdbank.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only view of attendance_records for reminder queries.
 */
@Entity
@Table(name = "attendance_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(AttendanceRecordViewId.class)
public class AttendanceRecordView {

    @Id
    private UUID id;

    @Id
    @Column(name = "check_time")
    private Instant checkTime;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "check_type")
    private String checkType;
}
