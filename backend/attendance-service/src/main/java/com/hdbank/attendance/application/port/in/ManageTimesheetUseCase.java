package com.hdbank.attendance.application.port.in;

import com.hdbank.attendance.domain.model.Timesheet;

import java.util.List;
import java.util.UUID;

public interface ManageTimesheetUseCase {

    Timesheet getOrCreateTimesheet(UUID employeeId, int month, int year);

    Timesheet calculateTimesheet(UUID employeeId, int month, int year);

    Timesheet submitForReview(UUID timesheetId);

    Timesheet approve(UUID timesheetId, UUID approverId);

    Timesheet lock(UUID timesheetId, UUID lockerId);

    List<Timesheet> getTimesheetsByManager(UUID managerId, int month, int year);
}
