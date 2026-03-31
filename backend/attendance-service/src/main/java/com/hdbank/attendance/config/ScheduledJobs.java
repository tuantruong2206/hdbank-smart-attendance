package com.hdbank.attendance.config;

import com.hdbank.attendance.application.port.out.EmployeeRepository;
import com.hdbank.attendance.application.port.out.LateGraceQuotaRepository;
import com.hdbank.attendance.domain.model.LateGraceQuota;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Scheduled jobs for the attendance service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobs {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int DEFAULT_LATE_GRACE_MAX = 4;

    private final LateGraceQuotaRepository lateGraceQuotaRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Monthly late grace quota reset.
     * Runs on the 1st of each month at 00:01 VN time.
     *
     * - Resets used_count = 0 for existing quota rows of the new month
     * - Creates new quota rows for employees that don't have one yet
     */
    @Scheduled(cron = "0 1 0 1 * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void resetMonthlyLateGraceQuota() {
        LocalDate today = LocalDate.now(VN_ZONE);
        int year = today.getYear();
        int month = today.getMonthValue();

        log.info("Starting monthly late grace quota reset for {}/{}", month, year);

        // 1. Reset existing quotas for this month (if any leftover from re-runs)
        int resetCount = lateGraceQuotaRepository.resetAllForNewMonth(year, month);
        log.info("Reset {} existing quota rows for {}/{}", resetCount, month, year);

        // 2. Find employees without a quota for this month and create one
        List<UUID> allEmployeeIds = employeeRepository.findAllActiveEmployeeIds();
        List<UUID> employeesWithQuota = lateGraceQuotaRepository
                .findEmployeeIdsWithoutQuota(year, month); // returns IDs WITH quota
        Set<UUID> withQuotaSet = new HashSet<>(employeesWithQuota);

        int createdCount = 0;
        for (UUID employeeId : allEmployeeIds) {
            if (!withQuotaSet.contains(employeeId)) {
                LateGraceQuota newQuota = LateGraceQuota.builder()
                        .id(UUID.randomUUID())
                        .employeeId(employeeId)
                        .year(year)
                        .month(month)
                        .maxAllowed(DEFAULT_LATE_GRACE_MAX)
                        .usedCount(0)
                        .build();
                lateGraceQuotaRepository.save(newQuota);
                createdCount++;
            }
        }

        log.info("Created {} new quota rows for {}/{}. Total employees: {}",
                createdCount, month, year, allEmployeeIds.size());
    }
}
