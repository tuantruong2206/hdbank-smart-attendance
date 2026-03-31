package com.hdbank.report.repository;

import com.hdbank.report.entity.AttendanceRecordView;
import com.hdbank.report.entity.AttendanceRecordViewId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AttendanceRecordViewRepository extends JpaRepository<AttendanceRecordView, AttendanceRecordViewId> {

    @Query("SELECT a FROM AttendanceRecordView a " +
           "WHERE a.checkTime BETWEEN :from AND :to ORDER BY a.checkTime ASC")
    List<AttendanceRecordView> findByTimeRange(Instant from, Instant to);

    @Query("SELECT a FROM AttendanceRecordView a " +
           "WHERE a.employeeId = :employeeId AND a.checkTime BETWEEN :from AND :to " +
           "ORDER BY a.checkTime ASC")
    List<AttendanceRecordView> findByEmployeeAndTimeRange(UUID employeeId, Instant from, Instant to);

    @Query("SELECT COUNT(DISTINCT a.employeeId) FROM AttendanceRecordView a " +
           "WHERE a.checkType = 'CHECK_IN' AND a.checkTime BETWEEN :from AND :to")
    long countDistinctCheckIns(Instant from, Instant to);

    @Query("SELECT COUNT(DISTINCT a.employeeId) FROM AttendanceRecordView a " +
           "WHERE a.checkType = 'CHECK_IN' AND a.status = 'SUSPICIOUS' " +
           "AND a.checkTime BETWEEN :from AND :to")
    long countSuspiciousCheckIns(Instant from, Instant to);
}
