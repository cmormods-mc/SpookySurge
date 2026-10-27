package com.spookysurge.surge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** The saved state is what lets a restart mid-surge carry on, so it is round-tripped through a real NBT tag. */
class SurgeStateTest {
    @Test
    void aRunningSurgeSurvivesASaveAndLoad() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        SurgeState before = new SurgeState();
        before.phase = SurgeState.Phase.ACTIVE;
        before.phaseEndsAt = 1_800_000_000_000L;
        before.surgeId = "abc-123";
        before.counter = 212;
        before.maxTier = 3;
        before.visitorSpawned = true;
        before.actions.put(alice, 7);
        before.names.put(alice, "Alice");
        before.pending.put(bob, 5);
        before.names.put(bob, "Bob");
        before.startedSlots.add("2026-10-31@20:00");

        SurgeState after = SurgeState.load(before.save(new CompoundTag(), null), null);

        assertEquals(SurgeState.Phase.ACTIVE, after.phase);
        assertEquals(1_800_000_000_000L, after.phaseEndsAt);
        assertEquals("abc-123", after.surgeId);
        assertEquals(212, after.counter);
        assertEquals(3, after.maxTier);
        assertTrue(after.visitorSpawned);
        assertEquals(7, after.actions.get(alice));
        assertEquals(5, after.pending.get(bob));
        assertEquals("Alice", after.names.get(alice));
        assertTrue(after.startedSlots.contains("2026-10-31@20:00"));
    }

    @Test
    void ownedPendingCandyIsKeptEvenForAPlayerWithNoActions() {
        UUID bob = UUID.randomUUID();
        SurgeState before = new SurgeState();
        before.pending.put(bob, 5);
        SurgeState after = SurgeState.load(before.save(new CompoundTag(), null), null);
        assertEquals(5, after.pending.get(bob));
        assertFalse(after.actions.containsKey(bob) && after.actions.get(bob) > 0);
    }

    @Test
    void anUnknownPhaseLoadsAsIdleInsteadOfCrashing() {
        CompoundTag tag = new SurgeState().save(new CompoundTag(), null);
        tag.putString("phase", "SOMETHING_FROM_A_FUTURE_VERSION");
        assertEquals(SurgeState.Phase.IDLE, SurgeState.load(tag, null).phase);
    }

    @Test
    void anEmptyTagLoadsAsAFreshIdleState() {
        SurgeState state = SurgeState.load(new CompoundTag(), null);
        assertEquals(SurgeState.Phase.IDLE, state.phase);
        assertEquals(0, state.counter);
        assertTrue(state.actions.isEmpty());
    }

    @Test
    void theLedgerIsRestoredFromSavedActions() {
        UUID alice = UUID.randomUUID();
        RewardLedger ledger = new RewardLedger(3);
        ledger.restore(Map.of(alice, 3));
        assertEquals(RewardLedger.Result.CAPPED, ledger.record(alice, UUID.randomUUID())); // already at the cap
    }
}
