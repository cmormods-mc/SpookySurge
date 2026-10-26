package com.spookysurge.surge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Who has been rewarded for what, for one surge. Two guards live here: a Pokemon pays at most once
 * (so a catch that also ends a battle cannot pay twice), and a player pays out at most {@code cap}
 * times (so catch-and-release farming earns nothing extra).
 */
public final class RewardLedger {
    private final Set<UUID> paidPokemon = new HashSet<>();
    private final Map<UUID, Integer> actions = new HashMap<>();
    private final int cap;

    public RewardLedger(int cap) {
        this.cap = cap;
    }

    /**
     * Records an action by {@code player} on {@code pokemon}. Returns true when it should pay: the
     * Pokemon has not paid before and the player is under the cap. The action is counted either way
     * the first time a Pokemon is seen, so participation still reflects real play.
     */
    public boolean tryPay(UUID player, UUID pokemon) {
        if (!paidPokemon.add(pokemon)) return false;
        int done = actions.merge(player, 1, Integer::sum);
        return done <= cap;
    }

    public int actionsBy(UUID player) {
        return actions.getOrDefault(player, 0);
    }

    public Map<UUID, Integer> snapshot() {
        return Map.copyOf(actions);
    }

    /** Restores counts after a restart. Pokemon ids are not restored: a surge Pokemon is removed at the end anyway. */
    public void restore(Map<UUID, Integer> saved) {
        actions.clear();
        actions.putAll(saved);
    }
}
