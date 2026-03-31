package com.hdbank.admin.adapter.in.web;

import com.hdbank.admin.adapter.out.persistence.entity.ConfigChangeJpaEntity;
import com.hdbank.admin.application.dto.ConfigChangeRequest;
import com.hdbank.admin.application.dto.ReviewRequest;
import com.hdbank.admin.application.service.MakerCheckerService;
import com.hdbank.common.dto.ApiResponse;
import com.hdbank.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/config-changes")
@RequiredArgsConstructor
public class ConfigChangeController {

    private final MakerCheckerService makerCheckerService;

    /**
     * Maker creates a config change request.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ConfigChangeJpaEntity>> requestChange(
            @RequestBody ConfigChangeRequest request) {
        ConfigChangeJpaEntity created = makerCheckerService.requestChange(request);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    /**
     * List all pending config changes for the checker.
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ConfigChangeJpaEntity>>> getPending() {
        return ResponseEntity.ok(ApiResponse.success(makerCheckerService.getPendingChanges()));
    }

    /**
     * Checker approves a pending change and triggers the actual config update.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ConfigChangeJpaEntity>> approve(
            @PathVariable UUID id,
            @RequestBody ReviewRequest review) {
        ConfigChangeJpaEntity approved = makerCheckerService.approveChange(id, review);
        return ResponseEntity.ok(ApiResponse.success("Đã duyệt thay đổi", approved));
    }

    /**
     * Checker rejects a pending change with a comment.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ConfigChangeJpaEntity>> reject(
            @PathVariable UUID id,
            @RequestBody ReviewRequest review) {
        ConfigChangeJpaEntity rejected = makerCheckerService.rejectChange(id, review);
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối thay đổi", rejected));
    }

    /**
     * Paginated history of all config changes.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<ConfigChangeJpaEntity>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(makerCheckerService.getHistory(page, size)));
    }
}
