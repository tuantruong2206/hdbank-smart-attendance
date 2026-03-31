package com.hdbank.report.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "report_attendance_daily",
       uniqueConstraints = @UniqueConstraint(columnNames = {"report_date", "organization_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAttendanceDailyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "organization_name")
    private String organizationName;

    @Column(name = "total_employees")
    @Builder.Default
    private int totalEmployees = 0;

    @Column(name = "present_count")
    @Builder.Default
    private int presentCount = 0;

    @Column(name = "absent_count")
    @Builder.Default
    private int absentCount = 0;

    @Column(name = "late_count")
    @Builder.Default
    private int lateCount = 0;

    @Column(name = "early_departure_count")
    @Builder.Default
    private int earlyDepartureCount = 0;

    @Column(name = "on_leave_count")
    @Builder.Default
    private int onLeaveCount = 0;

    @Column(name = "suspicious_count")
    @Builder.Default
    private int suspiciousCount = 0;

    @Column(name = "overtime_count")
    @Builder.Default
    private int overtimeCount = 0;

    @Column(name = "attendance_rate")
    @Builder.Default
    private double attendanceRate = 0.0;

    @Column(name = "on_time_rate")
    @Builder.Default
    private double onTimeRate = 0.0;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
