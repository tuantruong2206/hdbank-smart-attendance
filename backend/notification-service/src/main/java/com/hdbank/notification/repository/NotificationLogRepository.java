package com.hdbank.notification.repository;

import com.hdbank.notification.model.NotificationLogJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLogJpaEntity, UUID> {

    Page<NotificationLogJpaEntity> findByTargetEmployeeCode(String employeeCode, Pageable pageable);

    @Query("SELECT n FROM NotificationLogJpaEntity n " +
           "WHERE n.createdAt BETWEEN :from AND :to ORDER BY n.createdAt DESC")
    List<NotificationLogJpaEntity> findByDateRange(Instant from, Instant to);

    @Query("SELECT n FROM NotificationLogJpaEntity n " +
           "WHERE n.status = 'FAILED' AND n.createdAt > :since ORDER BY n.createdAt DESC")
    List<NotificationLogJpaEntity> findRecentFailures(Instant since);

    long countByEventTypeAndCreatedAtBetween(String eventType, Instant from, Instant to);
}
