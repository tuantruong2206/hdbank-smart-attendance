package com.hdbank.report.repository;

import com.hdbank.report.entity.ReportKpiDailyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ReportKpiDailyRepository extends JpaRepository<ReportKpiDailyJpaEntity, UUID> {

    Optional<ReportKpiDailyJpaEntity> findByReportDateAndOrganizationId(
            LocalDate reportDate, UUID organizationId);
}
