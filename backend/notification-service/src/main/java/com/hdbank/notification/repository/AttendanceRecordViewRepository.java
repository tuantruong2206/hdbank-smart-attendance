package com.hdbank.notification.repository;

import com.hdbank.notification.model.AttendanceRecordView;
import com.hdbank.notification.model.AttendanceRecordViewId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AttendanceRecordViewRepository extends JpaRepository<AttendanceRecordView, AttendanceRecordViewId> {

    @Query("SELECT a FROM AttendanceRecordView a " +
           "WHERE a.checkType = 'CHECK_IN' " +
           "AND a.checkTime BETWEEN :from AND :to")
    List<AttendanceRecordView> findCheckInsInRange(Instant from, Instant to);

    @Query("SELECT DISTINCT a.employeeId FROM AttendanceRecordView a " +
           "WHERE a.checkType = 'CHECK_OUT' " +
           "AND a.checkTime BETWEEN :from AND :to")
    List<UUID> findEmployeeIdsWithCheckOut(Instant from, Instant to);

    @Query("SELECT DISTINCT a.employeeId FROM AttendanceRecordView a " +
           "WHERE a.checkType = 'CHECK_IN' " +
           "AND a.checkTime BETWEEN :from AND :to")
    List<UUID> findEmployeeIdsWithCheckIn(Instant from, Instant to);
}
