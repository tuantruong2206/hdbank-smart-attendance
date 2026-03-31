package com.hdbank.admin.adapter.in.web;

import com.hdbank.admin.adapter.out.persistence.entity.EscalationRuleJpaEntity;
import com.hdbank.admin.adapter.out.persistence.repository.EscalationRuleJpaRepository;
import com.hdbank.common.dto.ApiResponse;
import com.hdbank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/escalation-rules")
@RequiredArgsConstructor
public class EscalationRuleController {

    private final EscalationRuleJpaRepository ruleRepo;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EscalationRuleJpaEntity>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(ruleRepo.findByIsActiveTrue()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EscalationRuleJpaEntity>> create(
            @RequestBody EscalationRuleJpaEntity rule) {
        return ResponseEntity.ok(ApiResponse.success(ruleRepo.save(rule)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EscalationRuleJpaEntity>> update(
            @PathVariable UUID id, @RequestBody EscalationRuleJpaEntity rule) {
        if (!ruleRepo.existsById(id)) {
            throw new ResourceNotFoundException("EscalationRule", id.toString());
        }
        rule.setId(id);
        return ResponseEntity.ok(ApiResponse.success(ruleRepo.save(rule)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        EscalationRuleJpaEntity rule = ruleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EscalationRule", id.toString()));
        rule.setActive(false);
        ruleRepo.save(rule);
        return ResponseEntity.ok(ApiResponse.success("Đã vô hiệu hóa quy tắc escalation", null));
    }
}
