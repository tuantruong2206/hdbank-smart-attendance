package com.hdbank.notification.service;

import com.hdbank.notification.model.EmployeeJpaEntity;
import com.hdbank.notification.repository.AttendanceRecordViewRepository;
import com.hdbank.notification.repository.EmployeeNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Scheduled reminders for check-in and check-out.
 * - 07:45 daily: check-in reminder to all active employees
 * - 16:45 daily: check-out reminder to employees who checked in but haven't checked out
 * Skips weekends (Saturday, Sunday).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final NotificationDispatcher dispatcher;
    private final EmployeeNotificationRepository employeeRepository;
    private final AttendanceRecordViewRepository attendanceRecordViewRepository;

    /**
     * Check-in reminder at 07:45 every weekday (Mon-Fri).
     */
    @Scheduled(cron = "0 45 7 * * MON-FRI")
    public void sendCheckInReminder() {
        log.info("Sending check-in reminders");

        if (isHoliday(LocalDate.now())) {
            log.info("Today is a holiday, skipping check-in reminders");
            return;
        }

        List<EmployeeJpaEntity> activeEmployees = employeeRepository.findAllActive();
        int count = 0;

        for (EmployeeJpaEntity employee : activeEmployees) {
            try {
                dispatcher.dispatch(
                        "PUSH",
                        employee.getEmployeeCode(),
                        "Nhắc nhở chấm công",
                        "Chào " + employee.getFullName() + ", đừng quên chấm công vào ca nhé!",
                        "NORMAL",
                        "REMINDER"
                );
                count++;
            } catch (Exception e) {
                log.error("Failed to send check-in reminder to {}: {}",
                        employee.getEmployeeCode(), e.getMessage());
            }
        }

        log.info("Check-in reminders sent to {} employees", count);
    }

    /**
     * Check-out reminder at 16:45 every weekday (Mon-Fri).
     * Only sends to employees who checked in but haven't checked out today.
     */
    @Scheduled(cron = "0 45 16 * * MON-FRI")
    public void sendCheckOutReminder() {
        log.info("Sending check-out reminders");

        if (isHoliday(LocalDate.now())) {
            log.info("Today is a holiday, skipping check-out reminders");
            return;
        }

        LocalDate today = LocalDate.now();
        Instant dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Find employees who checked in today
        List<UUID> checkedInIds = attendanceRecordViewRepository
                .findEmployeeIdsWithCheckIn(dayStart, dayEnd);

        // Find employees who already checked out
        Set<UUID> checkedOutIds = new HashSet<>(
                attendanceRecordViewRepository.findEmployeeIdsWithCheckOut(dayStart, dayEnd));

        int count = 0;
        for (UUID employeeId : checkedInIds) {
            if (checkedOutIds.contains(employeeId)) {
                continue; // Already checked out
            }

            try {
                EmployeeJpaEntity employee = employeeRepository.findById(employeeId).orElse(null);
                if (employee == null || !employee.isActive()) continue;

                dispatcher.dispatch(
                        "PUSH",
                        employee.getEmployeeCode(),
                        "Nhắc nhở chấm công ra",
                        "Chào " + employee.getFullName()
                                + ", ca làm việc sắp kết thúc. Đừng quên chấm công ra nhé!",
                        "NORMAL",
                        "REMINDER"
                );
                count++;
            } catch (Exception e) {
                log.error("Failed to send check-out reminder to employee {}: {}",
                        employeeId, e.getMessage());
            }
        }

        log.info("Check-out reminders sent to {} employees", count);
    }

    /**
     * Simple holiday check. In production this would query a holidays table.
     * Currently only skips weekends (handled by cron) and known Vietnamese holidays.
     */
    private boolean isHoliday(LocalDate date) {
        // Vietnamese public holidays (approximate fixed dates)
        MonthDay md = MonthDay.from(date);
        return md.equals(MonthDay.of(1, 1))   // Tết Dương lịch
            || md.equals(MonthDay.of(4, 30))   // Ngày Giải phóng
            || md.equals(MonthDay.of(5, 1))    // Quốc tế Lao động
            || md.equals(MonthDay.of(9, 2));   // Quốc khánh
        // Tết Nguyên đán and other lunar holidays would need a calendar lookup
    }
}
