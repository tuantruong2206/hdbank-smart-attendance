package com.hdbank.report.repository;

import com.hdbank.report.entity.ReportAttendanceDailyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportAttendanceDailyRepository extends JpaRepository<ReportAttendanceDailyJpaEntity, UUID> {

    Optional<ReportAttendanceDailyJpaEntity> findByReportDateAndOrganizationId(
            LocalDate reportDate, UUID organizationId);

    List<ReportAttendanceDailyJpaEntity> findByReportDate(LocalDate reportDate);

    @Query("SELECT r FROM ReportAttendanceDailyJpaEntity r " +
           "WHERE r.organizationId = :orgId AND r.reportDate BETWEEN :from AND :to " +
           "ORDER BY r.reportDate ASC")
    List<ReportAttendanceDailyJpaEntity> findByOrgAndDateRange(UUID orgId, LocalDate from, LocalDate to);

    @Query("SELECT r FROM ReportAttendanceDailyJpaEntity r " +
           "WHERE r.reportDate = :date ORDER BY r.attendanceRate ASC")
    List<ReportAttendanceDailyJpaEntity> findAllByDateOrderByRate(LocalDate date);
}
