package com.bossbarskins.api;

import net.minecraft.world.BossEvent;

/**
 * Optional. Mods do not need to call anything: a bar is attributed to the mod whose class called {@code new ServerBossEvent(...)}.
 * Use {@link #claim} only when shared code (a helper library) creates bars on behalf of another mod.
 */
public final class BossBarSkinsApi {
    private BossBarSkinsApi() {}

    /** Attributes the bar to {@code modId}. Call before the bar is first sent to a player. */
    public static void claim(BossEvent event, String modId) {
        ((OwnerHolder) event).bossbarskins$setOwner(modId);
    }

    public static String ownerOf(BossEvent event) {
        return ((OwnerHolder) event).bossbarskins$getOwner();
    }
}
