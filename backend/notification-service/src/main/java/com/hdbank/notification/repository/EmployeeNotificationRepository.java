package com.hdbank.notification.repository;

import com.hdbank.notification.model.EmployeeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EmployeeNotificationRepository extends JpaRepository<EmployeeJpaEntity, UUID> {

    @Query("SELECT e FROM EmployeeJpaEntity e WHERE e.isActive = true")
    List<EmployeeJpaEntity> findAllActive();

    @Query("SELECT e FROM EmployeeJpaEntity e WHERE e.organizationId = :orgId AND e.isActive = true")
    List<EmployeeJpaEntity> findActiveByOrganizationId(UUID orgId);
}
