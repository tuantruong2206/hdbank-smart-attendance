package com.hdbank.report.repository;

import com.hdbank.report.entity.OrganizationView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OrganizationViewRepository extends JpaRepository<OrganizationView, UUID> {

    @Query("SELECT o FROM OrganizationView o WHERE o.isActive = true")
    List<OrganizationView> findAllActive();

    List<OrganizationView> findByParentId(UUID parentId);

    @Query("SELECT o FROM OrganizationView o WHERE o.type = 'BRANCH' AND o.isActive = true")
    List<OrganizationView> findAllActiveBranches();
}
