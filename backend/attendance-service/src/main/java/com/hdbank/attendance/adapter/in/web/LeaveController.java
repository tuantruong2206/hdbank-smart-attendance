package com.hdbank.attendance.adapter.in.web;

import com.hdbank.attendance.adapter.in.web.request.CreateLeaveRequest;
import com.hdbank.attendance.adapter.in.web.request.ApproveLeaveRequest;
import com.hdbank.attendance.adapter.in.web.request.RejectLeaveRequest;
import com.hdbank.attendance.application.dto.CreateLeaveCommand;
import com.hdbank.attendance.application.port.in.LeaveRequestUseCase;
import com.hdbank.attendance.domain.model.LeaveBalance;
import com.hdbank.attendance.domain.model.LeaveRequest;
import com.hdbank.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveRequestUseCase leaveRequestUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<LeaveRequest>> createLeaveRequest(
            @Valid @RequestBody CreateLeaveRequest request,
            @RequestHeader("X-User-Id") String userId) {
        UUID employeeId = UUID.fromString(userId);
        CreateLeaveCommand command = new CreateLeaveCommand(
                employeeId,
                request.leaveType(),
                request.startDate(),
                request.endDate(),
                request.reason()
        );
        LeaveRequest result = leaveRequestUseCase.createLeaveRequest(command);
        return ResponseEntity.ok(ApiResponse.success("Tạo đơn nghỉ phép thành công", result));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getMyLeaveRequests(
            @RequestHeader("X-User-Id") String userId) {
        UUID employeeId = UUID.fromString(userId);
        List<LeaveRequest> requests = leaveRequestUseCase.getMyLeaveRequests(employeeId);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getPendingApprovals(
            @RequestHeader("X-User-Id") String userId) {
        UUID approverId = UUID.fromString(userId);
        List<LeaveRequest> requests = leaveRequestUseCase.getPendingApprovals(approverId);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<List<LeaveBalance>>> getLeaveBalance(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int year) {
        UUID employeeId = UUID.fromString(userId);
        if (year == 0) {
            year = LocalDate.now().getYear();
        }
        List<LeaveBalance> balances = leaveRequestUseCase.getLeaveBalance(employeeId, year);
        return ResponseEntity.ok(ApiResponse.success(balances));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<LeaveRequest>> approveLeave(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody(required = false) ApproveLeaveRequest request) {
        UUID approverId = UUID.fromString(userId);
        String comment = request != null ? request.comment() : null;
        LeaveRequest result = leaveRequestUseCase.approveLeave(id, approverId, comment);
        return ResponseEntity.ok(ApiResponse.success("Duyệt đơn nghỉ phép thành công", result));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<LeaveRequest>> rejectLeave(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody RejectLeaveRequest request) {
        UUID approverId = UUID.fromString(userId);
        LeaveRequest result = leaveRequestUseCase.rejectLeave(id, approverId, request.comment(), request.reason());
        return ResponseEntity.ok(ApiResponse.success("Từ chối đơn nghỉ phép thành công", result));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<LeaveRequest>> cancelLeave(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        UUID employeeId = UUID.fromString(userId);
        LeaveRequest result = leaveRequestUseCase.cancelLeave(id, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn nghỉ phép thành công", result));
    }
}
