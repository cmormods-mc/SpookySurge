package com.spookysurge.surge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Who has been rewarded for what, for one surge. Two guards live here: a Pokemon counts at most once
 * (so a catch that also ends a battle cannot count twice), and a player is paid for at most {@code cap}
 * Pokemon (so catch-and-release farming earns nothing extra, though it still moves the shared meter).
 */
public final class RewardLedger {
    public enum Result { PAID, CAPPED, DUPLICATE }

    private final Set<UUID> seen = new HashSet<>();
    private final Map<UUID, Integer> actions = new HashMap<>();
    private final int cap;

    public RewardLedger(int cap) {
        this.cap = cap;
    }

    public Result record(UUID player, UUID pokemon) {
        if (!seen.add(pokemon)) return Result.DUPLICATE;
        int done = actions.merge(player, 1, Integer::sum);
        return done <= cap ? Result.PAID : Result.CAPPED;
    }

    public int actionsBy(UUID player) {
        return actions.getOrDefault(player, 0);
    }

    public Map<UUID, Integer> snapshot() {
        return Map.copyOf(actions);
    }

    /** Restores counts after a restart. Pokemon ids are not restored: surge Pokemon are removed with the surge. */
    public void restore(Map<UUID, Integer> saved) {
        actions.clear();
        actions.putAll(saved);
    }
}
