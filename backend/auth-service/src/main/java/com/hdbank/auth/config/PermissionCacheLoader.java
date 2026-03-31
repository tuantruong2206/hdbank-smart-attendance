package com.hdbank.auth.config;

import com.hdbank.auth.adapter.out.persistence.entity.PermissionJpaEntity;
import com.hdbank.auth.adapter.out.persistence.repository.PermissionJpaRepository;
import com.hdbank.common.security.PermissionChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Loads permissions from the database into the in-memory PermissionChecker cache
 * at application startup and refreshes periodically (every 5 minutes).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionCacheLoader {

    private final PermissionJpaRepository permissionRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        loadPermissions();
    }

    @Scheduled(fixedRate = 300_000) // 5 minutes
    public void refreshPermissions() {
        loadPermissions();
    }

    private void loadPermissions() {
        try {
            List<PermissionJpaEntity> entities = permissionRepository.findByIsActiveTrue();
            List<PermissionChecker.Permission> permissions = entities.stream()
                    .map(e -> new PermissionChecker.Permission(
                            e.getRole(), e.getAction(), e.getResource(), e.getScope()))
                    .toList();
            PermissionChecker.loadPermissions(permissions);
            log.info("Loaded {} permissions into PermissionChecker cache", permissions.size());
        } catch (Exception e) {
            log.error("Failed to load permissions from database: {}", e.getMessage());
        }
    }
}
