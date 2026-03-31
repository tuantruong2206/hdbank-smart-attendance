package com.hdbank.attendance.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EscalationEngineTest {

    private EscalationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new EscalationEngine();
    }

    private List<EscalationEngine.EscalationRule> buildRules() {
        UUID orgId = UUID.randomUUID();
        return List.of(
                new EscalationEngine.EscalationRule(
                        UUID.randomUUID(), orgId, "ABSENT", 1, "UNIT_HEAD", 30, true),
                new EscalationEngine.EscalationRule(
                        UUID.randomUUID(), orgId, "ABSENT", 2, "DEPT_HEAD", 60, true),
                new EscalationEngine.EscalationRule(
                        UUID.randomUUID(), orgId, "ABSENT", 3, "DIRECTOR", 120, true)
        );
    }

    @Test
    @DisplayName("no time elapsed returns level 0")
    void noTimeElapsed_level0() {
        assertEquals(0, engine.determineEscalationLevel(buildRules(), 0));
    }

    @Test
    @DisplayName("after first timeout (30 min) escalates to level 1")
    void afterFirstTimeout_level1() {
        assertEquals(1, engine.determineEscalationLevel(buildRules(), 30));
    }

    @Test
    @DisplayName("after cumulative 90 min (30+60) escalates to level 2")
    void afterSecondTimeout_level2() {
        assertEquals(2, engine.determineEscalationLevel(buildRules(), 90));
    }

    @Test
    @DisplayName("after cumulative 210 min (30+60+120) escalates to level 3")
    void afterThirdTimeout_level3() {
        assertEquals(3, engine.determineEscalationLevel(buildRules(), 210));
    }

    @Test
    @DisplayName("between level 1 and level 2 timeout stays at level 1")
    void betweenLevels_staysAtLower() {
        // cumulative for level 1 = 30, for level 2 = 90
        assertEquals(1, engine.determineEscalationLevel(buildRules(), 60));
    }

    @Test
    @DisplayName("inactive rules are filtered out")
    void inactiveRules_filtered() {
        UUID orgId = UUID.randomUUID();
        List<EscalationEngine.EscalationRule> rules = List.of(
                new EscalationEngine.EscalationRule(
                        UUID.randomUUID(), orgId, "ABSENT", 1, "UNIT_HEAD", 30, true),
                new EscalationEngine.EscalationRule(
                        UUID.randomUUID(), orgId, "ABSENT", 2, "DEPT_HEAD", 60, false) // inactive
        );
        // Only level 1 is active; even at 200 min, should be level 1
        assertEquals(1, engine.determineEscalationLevel(rules, 200));
    }

    @Test
    @DisplayName("findTargetRole returns UNIT_HEAD for level 1")
    void findTargetRole_level1() {
        assertEquals(Optional.of("UNIT_HEAD"), engine.findTargetRole(1));
    }

    @Test
    @DisplayName("findTargetRole returns DEPT_HEAD for level 2")
    void findTargetRole_level2() {
        assertEquals(Optional.of("DEPT_HEAD"), engine.findTargetRole(2));
    }

    @Test
    @DisplayName("findTargetRole returns DIRECTOR for level 3")
    void findTargetRole_level3() {
        assertEquals(Optional.of("DIRECTOR"), engine.findTargetRole(3));
    }

    @Test
    @DisplayName("findTargetRole returns empty for unknown level")
    void findTargetRole_unknownLevel() {
        assertTrue(engine.findTargetRole(0).isEmpty());
        assertTrue(engine.findTargetRole(4).isEmpty());
    }

    @Test
    @DisplayName("shouldEscalate returns true when lastEscalationTime is null")
    void shouldEscalate_nullLastTime_true() {
        assertTrue(engine.shouldEscalate(null, 30));
    }

    @Test
    @DisplayName("shouldEscalate returns true when timeout exceeded")
    void shouldEscalate_timeoutExceeded_true() {
        Instant lastEscalation = Instant.now().minus(45, ChronoUnit.MINUTES);
        assertTrue(engine.shouldEscalate(lastEscalation, 30));
    }

    @Test
    @DisplayName("shouldEscalate returns false when timeout not reached")
    void shouldEscalate_timeoutNotReached_false() {
        Instant lastEscalation = Instant.now().minus(10, ChronoUnit.MINUTES);
        assertFalse(engine.shouldEscalate(lastEscalation, 30));
    }

    @Test
    @DisplayName("determineTriggerType returns SUSPICIOUS when suspicious")
    void triggerType_suspicious() {
        assertEquals("SUSPICIOUS", engine.determineTriggerType(true, false, true));
    }

    @Test
    @DisplayName("determineTriggerType returns ABSENT when not checked in")
    void triggerType_absent() {
        assertEquals("ABSENT", engine.determineTriggerType(false, false, false));
    }

    @Test
    @DisplayName("determineTriggerType returns LATE when checked in late")
    void triggerType_late() {
        assertEquals("LATE", engine.determineTriggerType(true, true, false));
    }

    @Test
    @DisplayName("determineTriggerType returns null when no issue")
    void triggerType_noIssue() {
        assertNull(engine.determineTriggerType(true, false, false));
    }

    @Test
    @DisplayName("SUSPICIOUS takes priority over ABSENT and LATE")
    void triggerType_suspiciousPriority() {
        assertEquals("SUSPICIOUS", engine.determineTriggerType(false, true, true));
    }
}
