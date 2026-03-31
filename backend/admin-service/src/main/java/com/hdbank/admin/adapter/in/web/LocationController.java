package com.hdbank.admin.adapter.in.web;

import com.hdbank.admin.adapter.out.persistence.entity.LocationJpaEntity;
import com.hdbank.admin.adapter.out.persistence.repository.LocationJpaRepository;
import com.hdbank.common.dto.ApiResponse;
import com.hdbank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationJpaRepository locationRepo;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationJpaEntity>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(locationRepo.findByIsActiveTrue()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationJpaEntity>> getById(@PathVariable UUID id) {
        LocationJpaEntity location = locationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", id.toString()));
        return ResponseEntity.ok(ApiResponse.success(location));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LocationJpaEntity>> create(@RequestBody LocationJpaEntity location) {
        return ResponseEntity.ok(ApiResponse.success(locationRepo.save(location)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationJpaEntity>> update(
            @PathVariable UUID id, @RequestBody LocationJpaEntity location) {
        if (!locationRepo.existsById(id)) {
            throw new ResourceNotFoundException("Location", id.toString());
        }
        location.setId(id);
        return ResponseEntity.ok(ApiResponse.success(locationRepo.save(location)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        LocationJpaEntity location = locationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", id.toString()));
        location.setActive(false);
        locationRepo.save(location);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa địa điểm", null));
    }
}
