package com.hdbank.admin.adapter.in.web;

import com.hdbank.admin.adapter.out.persistence.entity.EmployeeJpaEntity;
import com.hdbank.admin.adapter.out.persistence.entity.ShiftJpaEntity;
import com.hdbank.admin.adapter.out.persistence.repository.EmployeeAdminJpaRepository;
import com.hdbank.admin.adapter.out.persistence.repository.ShiftJpaRepository;
import com.hdbank.common.dto.ApiResponse;
import com.hdbank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftJpaRepository shiftRepo;
    private final EmployeeAdminJpaRepository employeeRepo;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShiftJpaEntity>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(shiftRepo.findByIsActiveTrue()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShiftJpaEntity>> create(@RequestBody ShiftJpaEntity shift) {
        return ResponseEntity.ok(ApiResponse.success(shiftRepo.save(shift)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShiftJpaEntity>> update(
            @PathVariable UUID id, @RequestBody ShiftJpaEntity shift) {
        if (!shiftRepo.existsById(id)) {
            throw new ResourceNotFoundException("Shift", id.toString());
        }
        shift.setId(id);
        return ResponseEntity.ok(ApiResponse.success(shiftRepo.save(shift)));
    }

    /**
     * Get the current shift for an employee based on their organization.
     * Looks up the employee's organizationId, then finds shifts assigned to that org.
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<ShiftJpaEntity>>> getEmployeeShift(
            @PathVariable UUID employeeId) {
        EmployeeJpaEntity employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId.toString()));
        List<ShiftJpaEntity> shifts = shiftRepo.findByOrganizationId(employee.getOrganizationId());
        if (shifts.isEmpty()) {
            // Fall back to system-wide shifts (no org assigned)
            shifts = shiftRepo.findByIsActiveTrue().stream()
                    .filter(s -> s.getOrganizationId() == null)
                    .toList();
        }
        return ResponseEntity.ok(ApiResponse.success(shifts));
    }
}
