package com.spookysurge.hud;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Lets an operator look at the Revenant bar without a surge running: {@code /surge bar preview}. The bar goes to that one player and
 * removes itself after a minute. It is also the quickest way to check on staging whether the resource pack reached a client.
 */
public final class RevenantPreview {
    private RevenantPreview() {}

    public static final String DEFAULT_NAME = "THE CANDY CORN REVENANT";
    private static final int LIFETIME_TICKS = 60 * 20;

    private record Entry(RevenantBar bar, long expiresAt) {}

    private static final Map<UUID, Entry> PREVIEWS = new HashMap<>();
    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(RevenantPreview::tick);
    }

    /** Shows a preview to one player, replacing any earlier preview of theirs. Returns the mode the player is shown. */
    public static RevenantBar.Mode show(ServerPlayer player, int percent, int tier, String name) {
        clear(player);
        RevenantBar bar = new RevenantBar(name, name);
        bar.setTier(tier);
        bar.setProgress(percent / 100f);
        bar.addViewer(player);
        PREVIEWS.put(player.getUUID(), new Entry(bar, player.getServer().getTickCount() + LIFETIME_TICKS));
        return bar.modeOf(player);
    }

    public static boolean clear(ServerPlayer player) {
        Entry old = PREVIEWS.remove(player.getUUID());
        if (old == null) return false;
        old.bar().close();
        return true;
    }

    private static void tick(MinecraftServer server) {
        if (PREVIEWS.isEmpty()) return;
        long now = server.getTickCount();
        for (Iterator<Entry> it = PREVIEWS.values().iterator(); it.hasNext(); ) {
            Entry e = it.next();
            if (now >= e.expiresAt() || e.bar().viewerCount() == 0) {
                e.bar().close();
                it.remove();
            }
        }
    }
}
