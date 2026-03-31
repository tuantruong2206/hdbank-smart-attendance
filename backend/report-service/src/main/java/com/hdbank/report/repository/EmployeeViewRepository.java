package com.hdbank.report.repository;

import com.hdbank.report.entity.EmployeeView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EmployeeViewRepository extends JpaRepository<EmployeeView, UUID> {

    @Query("SELECT COUNT(e) FROM EmployeeView e WHERE e.isActive = true")
    long countActive();

    @Query("SELECT COUNT(e) FROM EmployeeView e WHERE e.organizationId = :orgId AND e.isActive = true")
    long countActiveByOrganization(UUID orgId);

    @Query("SELECT e FROM EmployeeView e WHERE e.organizationId = :orgId AND e.isActive = true")
    List<EmployeeView> findActiveByOrganization(UUID orgId);
}
