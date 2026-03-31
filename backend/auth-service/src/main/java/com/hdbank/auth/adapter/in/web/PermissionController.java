package com.hdbank.auth.adapter.in.web;

import com.hdbank.auth.adapter.out.persistence.entity.PermissionJpaEntity;
import com.hdbank.auth.adapter.out.persistence.repository.PermissionJpaRepository;
import com.hdbank.common.dto.ApiResponse;
import com.hdbank.common.security.PermissionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionJpaRepository permissionRepository;

    /**
     * GET /api/v1/auth/permissions?role=EMPLOYEE
     * Returns the list of permissions for the specified role.
     * Used by the web frontend to check permissions client-side.
     */
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissions(
            @RequestParam String role) {

        List<PermissionJpaEntity> entities = permissionRepository.findByRoleAndIsActiveTrue(role);
        List<PermissionResponse> permissions = entities.stream()
                .map(e -> new PermissionResponse(e.getRole(), e.getAction(), e.getResource(), e.getScope()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    public record PermissionResponse(String role, String action, String resource, String scope) {
    }
}
