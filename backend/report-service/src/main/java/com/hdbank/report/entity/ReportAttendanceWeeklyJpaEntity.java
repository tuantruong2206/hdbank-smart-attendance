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
@Table(name = "report_attendance_weekly",
       uniqueConstraints = @UniqueConstraint(columnNames = {"week_start", "organization_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAttendanceWeeklyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "organization_name")
    private String organizationName;

    @Column(name = "avg_attendance_rate")
    @Builder.Default
    private double avgAttendanceRate = 0.0;

    @Column(name = "avg_on_time_rate")
    @Builder.Default
    private double avgOnTimeRate = 0.0;

    @Column(name = "total_late_count")
    @Builder.Default
    private int totalLateCount = 0;

    @Column(name = "total_absent_count")
    @Builder.Default
    private int totalAbsentCount = 0;

    @Column(name = "total_leave_count")
    @Builder.Default
    private int totalLeaveCount = 0;

    @Column(name = "total_suspicious_count")
    @Builder.Default
    private int totalSuspiciousCount = 0;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
