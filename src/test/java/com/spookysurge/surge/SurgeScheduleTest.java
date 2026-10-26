package com.spookysurge.surge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.spookysurge.config.SurgeConfig;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SurgeScheduleTest {
    private final SurgeConfig config = new SurgeConfig(); // 20:00 New York, window 2026-10-31T04Z..2026-11-03T05Z

    // 20:00 EDT on 2026-10-31 is 00:00Z on 2026-11-01.
    private static final Instant SLOT = Instant.parse("2026-11-01T00:00:00Z");

    @Test
    void nothingIsDueOutsideTheWindow() {
        assertTrue(SurgeSchedule.due(Instant.parse("2026-10-30T23:59:00Z"), config, Set.of()).isEmpty());
        assertTrue(SurgeSchedule.due(Instant.parse("2026-11-03T05:00:00Z"), config, Set.of()).isEmpty());
    }

    @Test
    void theCountdownStartsOneMinuteBeforeTheSlot() {
        assertTrue(SurgeSchedule.due(SLOT.minusSeconds(61), config, Set.of()).isEmpty());
        var due = SurgeSchedule.due(SLOT.minusSeconds(60), config, Set.of());
        assertEquals(SLOT, due.orElseThrow().start());
        assertEquals("2026-10-31@20:00", due.orElseThrow().key());
    }

    @Test
    void aLateBootStillJoinsTheRunningSurge() {
        assertTrue(SurgeSchedule.due(SLOT.plusSeconds(10 * 60), config, Set.of()).isPresent());
        assertTrue(SurgeSchedule.due(SLOT.plusSeconds(30 * 60), config, Set.of()).isEmpty()); // already over
    }

    @Test
    void aStartedSlotIsNotStartedTwice() {
        assertTrue(SurgeSchedule.due(SLOT, config, Set.of("2026-10-31@20:00")).isEmpty());
    }

    @Test
    void aBadTimeOrZoneDoesNotThrow() {
        config.startTimes = new java.util.ArrayList<>(java.util.List.of("not a time", "20:00"));
        config.timezone = "Mars/Olympus";
        SurgeSchedule.due(SLOT, config, Set.of()); // must not throw
    }
}
