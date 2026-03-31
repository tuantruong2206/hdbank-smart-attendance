package com.hdbank.admin.adapter.in.web;

import com.hdbank.admin.adapter.out.persistence.entity.HolidayJpaEntity;
import com.hdbank.admin.adapter.out.persistence.repository.HolidayJpaRepository;
import com.hdbank.common.dto.ApiResponse;
import com.hdbank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayJpaRepository holidayRepo;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HolidayJpaEntity>>> getByYear(
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(holidayRepo.findByYear(targetYear)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HolidayJpaEntity>> create(@RequestBody HolidayJpaEntity holiday) {
        return ResponseEntity.ok(ApiResponse.success(holidayRepo.save(holiday)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HolidayJpaEntity>> update(
            @PathVariable UUID id, @RequestBody HolidayJpaEntity holiday) {
        if (!holidayRepo.existsById(id)) {
            throw new ResourceNotFoundException("Holiday", id.toString());
        }
        holiday.setId(id);
        return ResponseEntity.ok(ApiResponse.success(holidayRepo.save(holiday)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        if (!holidayRepo.existsById(id)) {
            throw new ResourceNotFoundException("Holiday", id.toString());
        }
        holidayRepo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa ngày lễ", null));
    }
}
