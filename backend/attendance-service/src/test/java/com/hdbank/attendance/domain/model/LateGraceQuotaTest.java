package com.hdbank.attendance.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LateGraceQuotaTest {

    @Test
    void hasRemaining_true_when_quota_left() {
        var quota = LateGraceQuota.builder().maxAllowed(4).usedCount(2).build();
        assertTrue(quota.hasRemaining());
    }

    @Test
    void hasRemaining_false_when_exhausted() {
        var quota = LateGraceQuota.builder().maxAllowed(4).usedCount(4).build();
        assertFalse(quota.hasRemaining());
    }

    @Test
    void use_increments_count() {
        var quota = LateGraceQuota.builder().maxAllowed(4).usedCount(2).build();
        quota.use();
        assertEquals(3, quota.getUsedCount());
    }

    @Test
    void use_when_exhausted_throws() {
        var quota = LateGraceQuota.builder().maxAllowed(4).usedCount(4).build();
        assertThrows(IllegalStateException.class, quota::use);
    }

    @Test
    void getRemainingCount_correct() {
        var quota = LateGraceQuota.builder().maxAllowed(4).usedCount(1).build();
        assertEquals(3, quota.getRemainingCount());
    }

    @Test
    void getRemainingCount_zero_when_full() {
        var quota = LateGraceQuota.builder().maxAllowed(4).usedCount(4).build();
        assertEquals(0, quota.getRemainingCount());
    }
}
