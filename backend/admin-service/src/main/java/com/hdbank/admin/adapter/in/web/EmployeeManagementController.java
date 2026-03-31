package com.hdbank.admin.adapter.in.web;

import com.hdbank.admin.adapter.out.persistence.entity.EmployeeJpaEntity;
import com.hdbank.admin.adapter.out.persistence.repository.EmployeeAdminJpaRepository;
import com.hdbank.common.dto.ApiResponse;
import com.hdbank.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
public class EmployeeManagementController {

    private final EmployeeAdminJpaRepository employeeRepo;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeJpaEntity>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<EmployeeJpaEntity> result = employeeRepo.findAll(PageRequest.of(page, size));
        PageResponse<EmployeeJpaEntity> response = PageResponse.<EmployeeJpaEntity>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeJpaEntity>> getById(@PathVariable UUID id) {
        return employeeRepo.findById(id)
                .map(emp -> ResponseEntity.ok(ApiResponse.success(emp)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeJpaEntity>> create(@RequestBody EmployeeJpaEntity employee) {
        return ResponseEntity.ok(ApiResponse.success(employeeRepo.save(employee)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeJpaEntity>> update(@PathVariable UUID id, @RequestBody EmployeeJpaEntity employee) {
        employee.setId(id);
        return ResponseEntity.ok(ApiResponse.success(employeeRepo.save(employee)));
    }
}
