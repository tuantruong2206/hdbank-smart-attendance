package com.hdbank.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "target_employee_id")
    private UUID targetEmployeeId;

    @Column(name = "target_employee_code")
    private String targetEmployeeCode;

    @Column(nullable = false)
    private String channel; // PUSH, EMAIL, SMS

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    @Builder.Default
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT

    @Column(name = "event_type")
    private String eventType; // NOTIFICATION, ESCALATION, ANOMALY, CHECK_IN, REMINDER

    @Column(nullable = false)
    @Builder.Default
    private String status = "SENT"; // SENT, FAILED, PENDING

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
