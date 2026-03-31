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
@Table(name = "generated_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedReportJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "report_type", nullable = false)
    private String reportType; // ATTENDANCE_EXCEL, TIMESHEET_PDF

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, GENERATING, COMPLETED, FAILED

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "minio_bucket")
    @Builder.Default
    private String minioBucket = "reports";

    @Column(name = "minio_object_key")
    private String minioObjectKey;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
