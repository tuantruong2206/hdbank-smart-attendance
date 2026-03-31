package com.hdbank.attendance.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TimesheetTest {

    private Timesheet draftTimesheet() {
        return Timesheet.builder().status(Timesheet.TimesheetStatus.DRAFT).build();
    }

    @Test
    void submitForReview_from_draft_succeeds() {
        var ts = draftTimesheet();
        ts.submitForReview();
        assertEquals(Timesheet.TimesheetStatus.PENDING_REVIEW, ts.getStatus());
    }

    @Test
    void submitForReview_from_approved_throws() {
        var ts = Timesheet.builder().status(Timesheet.TimesheetStatus.APPROVED).build();
        assertThrows(IllegalStateException.class, ts::submitForReview);
    }

    @Test
    void submitForReview_from_locked_throws() {
        var ts = Timesheet.builder().status(Timesheet.TimesheetStatus.LOCKED).build();
        assertThrows(IllegalStateException.class, ts::submitForReview);
    }

    @Test
    void approve_from_pending_review_succeeds() {
        var ts = Timesheet.builder().status(Timesheet.TimesheetStatus.PENDING_REVIEW).build();
        UUID approverId = UUID.randomUUID();
        ts.approve(approverId);
        assertEquals(Timesheet.TimesheetStatus.APPROVED, ts.getStatus());
        assertEquals(approverId, ts.getApprovedBy());
        assertNotNull(ts.getApprovedAt());
    }

    @Test
    void approve_from_draft_throws() {
        var ts = draftTimesheet();
        assertThrows(IllegalStateException.class, () -> ts.approve(UUID.randomUUID()));
    }

    @Test
    void lock_from_approved_succeeds() {
        var ts = Timesheet.builder().status(Timesheet.TimesheetStatus.APPROVED).build();
        UUID lockerId = UUID.randomUUID();
        Map<String, Object> snapshot = Map.of("records", 20);
        ts.lock(lockerId, snapshot);
        assertEquals(Timesheet.TimesheetStatus.LOCKED, ts.getStatus());
        assertEquals(lockerId, ts.getLockedBy());
        assertNotNull(ts.getLockedAt());
        assertEquals(snapshot, ts.getSnapshot());
    }

    @Test
    void lock_from_draft_throws() {
        var ts = draftTimesheet();
        assertThrows(IllegalStateException.class, () -> ts.lock(UUID.randomUUID(), Map.of()));
    }

    @Test
    void rejectReview_from_pending_returns_to_draft() {
        var ts = Timesheet.builder().status(Timesheet.TimesheetStatus.PENDING_REVIEW).build();
        ts.rejectReview(UUID.randomUUID());
        assertEquals(Timesheet.TimesheetStatus.DRAFT, ts.getStatus());
    }

    @Test
    void rejectReview_from_approved_throws() {
        var ts = Timesheet.builder().status(Timesheet.TimesheetStatus.APPROVED).build();
        assertThrows(IllegalStateException.class, () -> ts.rejectReview(UUID.randomUUID()));
    }

    @Test
    void full_lifecycle_draft_to_locked() {
        var ts = draftTimesheet();
        ts.submitForReview();
        ts.approve(UUID.randomUUID());
        ts.lock(UUID.randomUUID(), Map.of("total", 22));
        assertEquals(Timesheet.TimesheetStatus.LOCKED, ts.getStatus());
    }
}
