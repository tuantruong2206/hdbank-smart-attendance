package com.hdbank.admin.adapter.in.web;

import com.hdbank.admin.adapter.out.persistence.entity.AuditLogJpaEntity;
import com.hdbank.admin.adapter.out.persistence.repository.AuditLogJpaRepository;
import com.hdbank.common.dto.ApiResponse;
import com.hdbank.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogJpaRepository auditLogRepo;

    /**
     * Paginated list of audit logs with optional filters.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogJpaEntity>>> getAll(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Instant fromInstant = from != null ? from.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;

        Page<AuditLogJpaEntity> result = auditLogRepo.findWithFilters(
                userId, action, resource, fromInstant, toInstant,
                PageRequest.of(page, size));

        PageResponse<AuditLogJpaEntity> response = PageResponse.<AuditLogJpaEntity>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
