package com.spookysurge.api;

import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired on the server thread when a surge that was running ends, for any reason. Other mods can
 * listen without Spooky Surge depending on them, for example to pay a bonus to the players who took
 * part. A listener that throws is contained and logged; it cannot stop the surge from ending.
 */
@FunctionalInterface
public interface SurgeEndedCallback {
    Event<SurgeEndedCallback> EVENT = EventFactory.createArrayBacked(SurgeEndedCallback.class, listeners -> summary -> {
        for (SurgeEndedCallback listener : listeners) {
            try {
                listener.onSurgeEnded(summary);
            } catch (RuntimeException e) {
                com.spookysurge.SpookySurge.LOG.error("A SurgeEndedCallback listener failed", e);
            }
        }
    });

    void onSurgeEnded(Summary summary);

    /** {@code actions} maps each player to how many surge Pokemon they caught or defeated. */
    record Summary(String surgeId, int tier, int counter, int target, Map<UUID, Integer> actions) {}
}
