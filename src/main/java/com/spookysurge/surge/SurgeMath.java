package com.spookysurge.surge;

import com.spookysurge.config.SurgeConfig;
import java.util.List;
import java.util.Random;

/** The rules of a surge with no Minecraft types in them, so they can be unit tested in a second. */
public final class SurgeMath {
    private SurgeMath() {}

    /** The shared counter's target for the number of players online, so a quiet night is still winnable. */
    public static int target(int online, SurgeConfig config) {
        long raw = (long) Math.max(1, online) * config.targetPerPlayer;
        return (int) Math.max(config.targetMin, Math.min(config.targetMax, raw));
    }

    /** The tier (0..4) a counter reaches against a target. Tiers unlock at the configured fractions. */
    public static int tierFor(int counter, int target, List<Double> fractions) {
        int tier = 0;
        for (int i = 0; i < fractions.size(); i++) {
            if (counter >= Math.ceil(target * fractions.get(i))) tier = i + 1;
        }
        return tier;
    }

    /** Candy Corn for one catch or defeat. Doubled (or more) once the table reaches the configured tier. */
    public static int candyFor(String rarity, int tier, SurgeConfig config) {
        int base = switch (rarity == null ? "" : rarity) {
            case "rare" -> config.candyRare;
            case "uncommon" -> config.candyUncommon;
            default -> config.candyCommon;
        };
        return tier >= config.doubleCandyTier ? base * config.doubleCandyMultiplier : base;
    }

    /** Spawn interval in ticks: faster from tier 1. */
    public static int spawnInterval(int tier, SurgeConfig config) {
        return tier >= 1 ? config.boostedSpawnIntervalTicks : config.spawnIntervalTicks;
    }

    /** A weighted pick from the species list, or null if it is empty. */
    public static SurgeConfig.SpeciesEntry pickSpecies(List<SurgeConfig.SpeciesEntry> species, Random random) {
        int total = 0;
        for (SurgeConfig.SpeciesEntry entry : species) total += Math.max(0, entry.weight);
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (SurgeConfig.SpeciesEntry entry : species) {
            roll -= Math.max(0, entry.weight);
            if (roll < 0) return entry;
        }
        return null;
    }
}
