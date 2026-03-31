package com.hdbank.attendance.config;

import com.hdbank.attendance.application.port.out.AttendanceRepository;
import com.hdbank.attendance.application.port.out.LocationRepository;
import com.hdbank.attendance.application.port.out.ShiftConfigRepository;
import com.hdbank.attendance.domain.service.ApprovalRouter;
import com.hdbank.attendance.domain.service.ConfigResolver;
import com.hdbank.attendance.domain.service.DuplicateCheckInGuard;
import com.hdbank.attendance.domain.service.EscalationEngine;
import com.hdbank.attendance.domain.service.FraudScorer;
import com.hdbank.attendance.domain.service.LocationVerifier;
import com.hdbank.attendance.domain.service.QrCodeGenerator;
import com.hdbank.attendance.domain.service.ShiftRuleEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Optional;

@Configuration
@EnableScheduling
public class BeanConfig {

    @Bean
    public LocationVerifier locationVerifier(LocationRepository locationRepo) {
        return new LocationVerifier(
                bssid -> locationRepo.findWifiAccessPointByBssid(bssid),
                id -> locationRepo.findById(id)
        );
    }

    @Bean
    public DuplicateCheckInGuard duplicateCheckInGuard(AttendanceRepository attendanceRepo) {
        return new DuplicateCheckInGuard(employeeId -> attendanceRepo.findLastCheckIn(employeeId));
    }

    @Bean
    public FraudScorer fraudScorer() {
        return new FraudScorer();
    }

    @Bean
    public EscalationEngine escalationEngine() {
        return new EscalationEngine();
    }

    @Bean
    public ApprovalRouter approvalRouter() {
        // Org node lookup and absence lookup — in production these would query
        // admin-service or a local org_units / absence table.
        return new ApprovalRouter(
                orgId -> Optional.empty(),
                employeeId -> Optional.empty()
        );
    }

    @Bean
    public ShiftRuleEngine shiftRuleEngine() {
        return new ShiftRuleEngine();
    }

    @Bean
    public QrCodeGenerator qrCodeGenerator() {
        return new QrCodeGenerator();
    }

    @Bean
    public ConfigResolver configResolver(ShiftConfigRepository shiftConfigRepo) {
        return new ConfigResolver(
                employeeId -> shiftConfigRepo.findOrgPathByEmployeeId(employeeId),
                orgPath -> shiftConfigRepo.findConfigsByOrgPathPrefix(orgPath),
                () -> shiftConfigRepo.findSystemDefault()
        );
    }
}
