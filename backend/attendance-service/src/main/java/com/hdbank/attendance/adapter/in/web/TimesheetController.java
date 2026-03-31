package com.hdbank.attendance.adapter.in.web;

import com.hdbank.attendance.application.port.in.ManageTimesheetUseCase;
import com.hdbank.attendance.domain.model.Timesheet;
import com.hdbank.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

    private final ManageTimesheetUseCase timesheetUseCase;

    @GetMapping("/{employeeId}/{year}/{month}")
    public ResponseEntity<ApiResponse<Timesheet>> getTimesheet(
            @PathVariable UUID employeeId,
            @PathVariable int year,
            @PathVariable int month) {
        Timesheet timesheet = timesheetUseCase.getOrCreateTimesheet(employeeId, month, year);
        return ResponseEntity.ok(ApiResponse.success(timesheet));
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<Timesheet>> calculate(
            @RequestParam UUID employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        Timesheet timesheet = timesheetUseCase.calculateTimesheet(employeeId, month, year);
        return ResponseEntity.ok(ApiResponse.success("Tính bảng công thành công", timesheet));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<Timesheet>> submitForReview(@PathVariable UUID id) {
        Timesheet timesheet = timesheetUseCase.submitForReview(id);
        return ResponseEntity.ok(ApiResponse.success("Gửi duyệt bảng công thành công", timesheet));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Timesheet>> approve(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        UUID approverId = UUID.fromString(userId);
        Timesheet timesheet = timesheetUseCase.approve(id, approverId);
        return ResponseEntity.ok(ApiResponse.success("Duyệt bảng công thành công", timesheet));
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<Timesheet>> lock(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        UUID lockerId = UUID.fromString(userId);
        Timesheet timesheet = timesheetUseCase.lock(id, lockerId);
        return ResponseEntity.ok(ApiResponse.success("Khóa bảng công thành công", timesheet));
    }

    @GetMapping("/team/{year}/{month}")
    public ResponseEntity<ApiResponse<List<Timesheet>>> getTeamTimesheets(
            @PathVariable int year,
            @PathVariable int month,
            @RequestHeader("X-User-Id") String userId) {
        UUID managerId = UUID.fromString(userId);
        List<Timesheet> timesheets = timesheetUseCase.getTimesheetsByManager(managerId, month, year);
        return ResponseEntity.ok(ApiResponse.success(timesheets));
    }
}
