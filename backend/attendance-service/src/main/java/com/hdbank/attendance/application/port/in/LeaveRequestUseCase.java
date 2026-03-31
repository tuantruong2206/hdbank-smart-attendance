package com.hdbank.attendance.application.port.in;

import com.hdbank.attendance.application.dto.CreateLeaveCommand;
import com.hdbank.attendance.domain.model.LeaveBalance;
import com.hdbank.attendance.domain.model.LeaveRequest;

import java.util.List;
import java.util.UUID;

public interface LeaveRequestUseCase {

    LeaveRequest createLeaveRequest(CreateLeaveCommand command);

    LeaveRequest approveLeave(UUID requestId, UUID approverId, String comment);

    LeaveRequest rejectLeave(UUID requestId, UUID approverId, String comment, String reason);

    LeaveRequest cancelLeave(UUID requestId, UUID employeeId);

    List<LeaveRequest> getMyLeaveRequests(UUID employeeId);

    List<LeaveRequest> getPendingApprovals(UUID approverId);

    List<LeaveBalance> getLeaveBalance(UUID employeeId, int year);
}
