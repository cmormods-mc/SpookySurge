package com.spookysurge.surge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.spookysurge.config.SurgeConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SurgeMathTest {
    private final SurgeConfig config = new SurgeConfig();

    @Test
    void targetScalesWithPlayersAndIsClamped() {
        assertEquals(100, SurgeMath.target(0, config));   // 1 * 60 -> raised to min
        assertEquals(300, SurgeMath.target(5, config));
        assertEquals(1000, SurgeMath.target(500, config)); // clamped to max
    }

    @Test
    void tiersUnlockAtTheFractions() {
        List<Double> f = config.tierFractions;
        assertEquals(0, SurgeMath.tierFor(0, 400, f));
        assertEquals(0, SurgeMath.tierFor(99, 400, f));
        assertEquals(1, SurgeMath.tierFor(100, 400, f));
        assertEquals(2, SurgeMath.tierFor(200, 400, f));
        assertEquals(3, SurgeMath.tierFor(300, 400, f));
        assertEquals(4, SurgeMath.tierFor(400, 400, f));
        assertEquals(4, SurgeMath.tierFor(9999, 400, f));
    }

    @Test
    void candyIsDoubledFromTheConfiguredTier() {
        assertEquals(1, SurgeMath.candyFor("common", 2, config));
        assertEquals(2, SurgeMath.candyFor("common", 3, config));
        assertEquals(12, SurgeMath.candyFor("rare", 4, config));
        assertEquals(1, SurgeMath.candyFor("nonsense", 0, config)); // unknown rarity pays like common
    }

    @Test
    void spawnIsFasterFromTierOne() {
        assertEquals(100, SurgeMath.spawnInterval(0, config));
        assertEquals(60, SurgeMath.spawnInterval(1, config));
    }

    @Test
    void speciesPickRespectsWeights() {
        List<SurgeConfig.SpeciesEntry> pool = List.of(
                new SurgeConfig.SpeciesEntry("a", 9, "common"), new SurgeConfig.SpeciesEntry("b", 1, "rare"));
        Random random = new Random(7);
        int a = 0;
        for (int i = 0; i < 10_000; i++) if (SurgeMath.pickSpecies(pool, random).id.equals("a")) a++;
        assertTrue(a > 8500 && a < 9500, "expected about 90% 'a', got " + a);
        assertEquals(null, SurgeMath.pickSpecies(List.of(), random));
    }

    @Test
    void aPokemonPaysOnlyOnce() {
        RewardLedger ledger = new RewardLedger(60);
        UUID player = UUID.randomUUID();
        UUID pokemon = UUID.randomUUID();
        assertEquals(RewardLedger.Result.PAID, ledger.record(player, pokemon));
        assertEquals(RewardLedger.Result.DUPLICATE, ledger.record(player, pokemon));
        assertEquals(1, ledger.actionsBy(player));
    }

    @Test
    void aPlayerStopsPayingAtTheCapButStillCounts() {
        RewardLedger ledger = new RewardLedger(3);
        UUID player = UUID.randomUUID();
        for (int i = 0; i < 3; i++) assertEquals(RewardLedger.Result.PAID, ledger.record(player, UUID.randomUUID()));
        assertEquals(RewardLedger.Result.CAPPED, ledger.record(player, UUID.randomUUID()));
        assertEquals(4, ledger.actionsBy(player));
    }

    @Test
    void badConfigIsRepairedNotRejected() {
        SurgeConfig bad = new SurgeConfig();
        bad.durationMinutes = 0;
        bad.perPlayerCap = 9999;
        bad.tierFractions = new ArrayList<>(List.of(0.9, 0.5, 0.75, 1.0));
        bad.species = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        bad.validated(problems);
        assertEquals(1, bad.durationMinutes);
        assertEquals(20, bad.perPlayerCap);
        assertEquals(List.of(0.25, 0.5, 0.75, 1.0), bad.tierFractions);
        assertNotNull(SurgeMath.pickSpecies(bad.species, new Random(1)));
        assertTrue(problems.size() >= 4);
    }

    @Test
    void xpBonusIsOffByDefaultAndOnlyAppliesFromItsTier() {
        assertEquals(1000, SurgeMath.xpFor(1000, 4, config)); // disabled
        config.xpBonusEnabled = true;                          // 25% from tier 2
        assertEquals(1000, SurgeMath.xpFor(1000, 1, config));
        assertEquals(1250, SurgeMath.xpFor(1000, 2, config));
        assertEquals(0, SurgeMath.xpFor(0, 4, config));
        assertEquals(Integer.MAX_VALUE, SurgeMath.xpFor(Integer.MAX_VALUE, 4, config));
    }
}
