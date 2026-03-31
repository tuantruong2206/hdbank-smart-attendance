package com.hdbank.report.repository;

import com.hdbank.report.entity.GeneratedReportJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GeneratedReportRepository extends JpaRepository<GeneratedReportJpaEntity, UUID> {

    Page<GeneratedReportJpaEntity> findByRequestedByOrderByCreatedAtDesc(UUID requestedBy, Pageable pageable);

    Page<GeneratedReportJpaEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);
}
