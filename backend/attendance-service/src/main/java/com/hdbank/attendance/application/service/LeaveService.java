package com.hdbank.attendance.application.service;

import com.hdbank.attendance.application.dto.CreateLeaveCommand;
import com.hdbank.attendance.application.port.in.LeaveRequestUseCase;
import com.hdbank.attendance.application.port.out.EmployeeRepository;
import com.hdbank.attendance.application.port.out.EventPublisher;
import com.hdbank.attendance.application.port.out.LeaveBalanceRepository;
import com.hdbank.attendance.application.port.out.LeaveRequestRepository;
import com.hdbank.attendance.domain.model.LeaveBalance;
import com.hdbank.attendance.domain.model.LeaveRequest;
import com.hdbank.attendance.domain.model.LeaveRequest.LeaveStatus;
import com.hdbank.attendance.domain.service.ApprovalRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService implements LeaveRequestUseCase {

    private static final int DEFAULT_MAX_APPROVAL_LEVELS = 2;

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final EventPublisher eventPublisher;
    private final ApprovalRouter approvalRouter;

    @Override
    @Transactional
    public LeaveRequest createLeaveRequest(CreateLeaveCommand command) {
        // Calculate total days (simple: business days between start and end inclusive)
        int totalDays = calculateBusinessDays(command.startDate(), command.endDate());

        // Validate leave balance
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeAndYearAndType(command.employeeId(), command.startDate().getYear(), command.leaveType())
                .orElseThrow(() -> new NoSuchElementException(
                        "Không tìm thấy số dư phép loại " + command.leaveType() + " cho năm " + command.startDate().getYear()));

        balance.deduct(totalDays);
        leaveBalanceRepository.save(balance);

        // Find direct manager as first approver
        UUID firstApprover = employeeRepository.findDirectManagerId(command.employeeId())
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy quản lý trực tiếp cho nhân viên: " + command.employeeId()));

        // Check if approver needs rerouting (absent >3 days)
        if (approvalRouter.shouldAutoReroute(firstApprover)) {
            log.info("Người duyệt {} vắng mặt >3 ngày, tự động chuyển đơn", firstApprover);
            Optional<UUID> substitute = approvalRouter.findSubstituteApprover(firstApprover, List.of());
            if (substitute.isPresent()) {
                firstApprover = substitute.get();
            }
        }

        LeaveRequest request = LeaveRequest.builder()
                .id(UUID.randomUUID())
                .employeeId(command.employeeId())
                .leaveType(command.leaveType())
                .startDate(command.startDate())
                .endDate(command.endDate())
                .totalDays(totalDays)
                .reason(command.reason())
                .status(LeaveStatus.PENDING)
                .currentApproverId(firstApprover)
                .currentApprovalLevel(1)
                .maxApprovalLevel(DEFAULT_MAX_APPROVAL_LEVELS)
                .approvals(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        request = leaveRequestRepository.save(request);

        // Publish notification event to approver
        eventPublisher.publishAudit(Map.of(
                "type", "LEAVE_REQUEST_CREATED",
                "requestId", request.getId().toString(),
                "employeeId", command.employeeId().toString(),
                "approverId", firstApprover.toString(),
                "leaveType", command.leaveType(),
                "startDate", command.startDate().toString(),
                "endDate", command.endDate().toString()
        ));

        log.info("Tạo đơn nghỉ phép {} cho nhân viên {}, người duyệt: {}",
                request.getId(), command.employeeId(), firstApprover);
        return request;
    }

    @Override
    @Transactional
    public LeaveRequest approveLeave(UUID requestId, UUID approverId, String comment) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn nghỉ phép: " + requestId));

        if (!request.getCurrentApproverId().equals(approverId)) {
            throw new IllegalArgumentException("Bạn không phải người duyệt hiện tại cho đơn này");
        }

        request.approve(approverId, comment);

        // If still pending (multi-level), route to next approver
        if (request.getStatus() == LeaveStatus.PENDING) {
            Optional<UUID> nextApprover = approvalRouter.findNextApprover(
                    approverId, List.of());
            if (nextApprover.isPresent()) {
                request.setCurrentApproverId(nextApprover.get());
            } else {
                // No more approvers, auto-approve
                request.setStatus(LeaveStatus.APPROVED);
            }
        }

        // If fully approved, confirm the balance deduction
        if (request.getStatus() == LeaveStatus.APPROVED) {
            leaveBalanceRepository.findByEmployeeAndYearAndType(
                    request.getEmployeeId(), request.getStartDate().getYear(), request.getLeaveType()
            ).ifPresent(balance -> {
                balance.confirmUsed(request.getTotalDays());
                leaveBalanceRepository.save(balance);
            });

            eventPublisher.publishAudit(Map.of(
                    "type", "LEAVE_REQUEST_APPROVED",
                    "requestId", requestId.toString(),
                    "employeeId", request.getEmployeeId().toString()
            ));
        }

        request = leaveRequestRepository.save(request);
        log.info("Duyệt đơn nghỉ phép {} bởi {}, trạng thái: {}", requestId, approverId, request.getStatus());
        return request;
    }

    @Override
    @Transactional
    public LeaveRequest rejectLeave(UUID requestId, UUID approverId, String comment, String reason) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn nghỉ phép: " + requestId));

        if (!request.getCurrentApproverId().equals(approverId)) {
            throw new IllegalArgumentException("Bạn không phải người duyệt hiện tại cho đơn này");
        }

        request.reject(approverId, comment, reason);

        // Release the pending balance
        leaveBalanceRepository.findByEmployeeAndYearAndType(
                request.getEmployeeId(), request.getStartDate().getYear(), request.getLeaveType()
        ).ifPresent(balance -> {
            balance.releasePending(request.getTotalDays());
            leaveBalanceRepository.save(balance);
        });

        request = leaveRequestRepository.save(request);

        eventPublisher.publishAudit(Map.of(
                "type", "LEAVE_REQUEST_REJECTED",
                "requestId", requestId.toString(),
                "employeeId", request.getEmployeeId().toString(),
                "reason", reason
        ));

        log.info("Từ chối đơn nghỉ phép {} bởi {}, lý do: {}", requestId, approverId, reason);
        return request;
    }

    @Override
    @Transactional
    public LeaveRequest cancelLeave(UUID requestId, UUID employeeId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn nghỉ phép: " + requestId));

        request.cancel(employeeId);

        // Release the pending balance
        leaveBalanceRepository.findByEmployeeAndYearAndType(
                request.getEmployeeId(), request.getStartDate().getYear(), request.getLeaveType()
        ).ifPresent(balance -> {
            balance.releasePending(request.getTotalDays());
            leaveBalanceRepository.save(balance);
        });

        request = leaveRequestRepository.save(request);

        log.info("Hủy đơn nghỉ phép {} bởi nhân viên {}", requestId, employeeId);
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequest> getMyLeaveRequests(UUID employeeId) {
        return leaveRequestRepository.findByEmployeeId(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequest> getPendingApprovals(UUID approverId) {
        return leaveRequestRepository.findPendingByApproverId(approverId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalance> getLeaveBalance(UUID employeeId, int year) {
        return leaveBalanceRepository.findByEmployeeAndYear(employeeId, year);
    }

    private int calculateBusinessDays(java.time.LocalDate start, java.time.LocalDate end) {
        int count = 0;
        java.time.LocalDate current = start;
        while (!current.isAfter(end)) {
            int dow = current.getDayOfWeek().getValue();
            if (dow <= 5) {
                count++;
            }
            current = current.plusDays(1);
        }
        return Math.max(count, 1);
    }
}
