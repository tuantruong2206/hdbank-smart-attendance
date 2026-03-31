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
@Table(name = "report_kpi_daily",
       uniqueConstraints = @UniqueConstraint(columnNames = {"report_date", "organization_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportKpiDailyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "attendance_compliance_pct")
    @Builder.Default
    private double attendanceCompliancePct = 0.0;

    @Column(name = "on_time_pct")
    @Builder.Default
    private double onTimePct = 0.0;

    @Column(name = "fraud_incident_count")
    @Builder.Default
    private int fraudIncidentCount = 0;

    @Column(name = "avg_check_in_time_minutes")
    @Builder.Default
    private double avgCheckInTimeMinutes = 0.0; // minutes after midnight

    @Column(name = "pending_approvals_count")
    @Builder.Default
    private int pendingApprovalsCount = 0;

    @Column(name = "escalation_count")
    @Builder.Default
    private int escalationCount = 0;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
