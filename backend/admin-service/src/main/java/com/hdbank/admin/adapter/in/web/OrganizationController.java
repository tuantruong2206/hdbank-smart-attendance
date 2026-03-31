package com.hdbank.admin.adapter.in.web;

import com.hdbank.admin.adapter.out.persistence.entity.OrganizationJpaEntity;
import com.hdbank.admin.adapter.out.persistence.repository.OrganizationJpaRepository;
import com.hdbank.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationJpaRepository orgRepo;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationJpaEntity>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(orgRepo.findAll()));
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<OrganizationJpaEntity>>> getTree() {
        return ResponseEntity.ok(ApiResponse.success(orgRepo.findByIsActiveTrue()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationJpaEntity>> getById(@PathVariable UUID id) {
        return orgRepo.findById(id)
                .map(org -> ResponseEntity.ok(ApiResponse.success(org)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationJpaEntity>> create(@RequestBody OrganizationJpaEntity org) {
        return ResponseEntity.ok(ApiResponse.success(orgRepo.save(org)));
    }
}
