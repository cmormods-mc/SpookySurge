package com.spookysurge.surge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class WorldBossCandyTest {
    @Test
    void everyoneWhoDealtDamageGetsTheFlatAmount() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), idle = UUID.randomUUID();
        Map<UUID, Integer> owed = WorldBossCandy.payouts(Map.of(a, 900f, b, 0.5f, idle, 0f), 35);
        assertEquals(Map.of(a, 35, b, 35), owed);
    }

    @Test
    void zeroAmountPaysNobody() {
        assertTrue(WorldBossCandy.payouts(Map.of(UUID.randomUUID(), 100f), 0).isEmpty());
    }

    @Test
    void paidBossesSurviveASaveAndLoad() {
        SurgeState before = new SurgeState();
        before.paidBosses.add("boss-1");
        SurgeState after = SurgeState.load(before.save(new CompoundTag(), null), null);
        assertTrue(after.paidBosses.contains("boss-1"));
    }
}
