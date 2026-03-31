package com.hdbank.report.repository;

import com.hdbank.report.entity.ReportAttendanceWeeklyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportAttendanceWeeklyRepository extends JpaRepository<ReportAttendanceWeeklyJpaEntity, UUID> {

    Optional<ReportAttendanceWeeklyJpaEntity> findByWeekStartAndOrganizationId(
            LocalDate weekStart, UUID organizationId);

    List<ReportAttendanceWeeklyJpaEntity> findByWeekStart(LocalDate weekStart);

    List<ReportAttendanceWeeklyJpaEntity> findByOrganizationIdOrderByWeekStartDesc(UUID organizationId);
}
