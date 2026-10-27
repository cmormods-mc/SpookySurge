package com.spookysurge.hud;

import com.spookysurge.SpookySurge;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.BossEvent;

/**
 * The Candy Corn Revenant boss bar. Server thread only.
 *
 * <p>What a player sees depends on whether their client has this server's resource pack:
 * <ul>
 *   <li><b>ART</b> (pack loaded): three vanilla boss bars, added in this order: a BLUE spacer whose sprites the pack makes transparent
 *       (it only pushes the next row down 19 px, giving the art room above the bar), then a YELLOW bar whose sprites the pack replaces
 *       and whose title carries the frame, the tier lights and the name as font glyphs, then a second BLUE spacer so a boss bar from
 *       another source never sits on the ornaments that hang below the bar.</li>
 *   <li><b>PLAIN</b> (no pack, declined, still loading): one ordinary YELLOW bar with a readable text title.</li>
 * </ul>
 * YELLOW and BLUE are reserved for this bar: the pack's replacement sprites apply to every bar of those colours, on the whole server.
 * Progress is the vanilla bar's own fill, so it clips natively; nothing in the art depends on the fill position.
 */
public final class RevenantBar {
    public static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath(SpookySurge.MOD_ID, "revenant");

    /** How a viewer is shown: see the class comment. */
    public enum Mode { ART, PLAIN }

    /** Which mode to use for players: AUTO asks Polymer whether the player has this server's pack. */
    public enum Policy {
        AUTO, ART, PLAIN;

        public static Policy parse(String value) {
            if (value != null) for (Policy p : values()) if (p.name().equalsIgnoreCase(value.trim())) return p;
            return AUTO;
        }
    }

    private static final List<RevenantBar> LIVE = new ArrayList<>();
    private static Policy policy = Policy.AUTO;
    private static boolean hooked;

    private final RevenantTitle layout = new RevenantTitle(RevenantMetrics.load());
    private final ServerBossEvent spacer = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private final ServerBossEvent tail = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private final ServerBossEvent art;
    private final ServerBossEvent plain;
    private final Map<UUID, Mode> viewers = new HashMap<>();
    private String artName = "";
    private String plainName;
    private int tier;

    public RevenantBar(String artName, String plainName) {
        this.artName = artName;
        this.plainName = plainName;
        this.art = new ServerBossEvent(title(artName, 0), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        this.plain = new ServerBossEvent(Component.literal(plainName), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        this.spacer.setProgress(0f);
        this.tail.setProgress(0f);
        synchronized (LIVE) { LIVE.add(this); }
    }

    // ------------------------------------------------------------------------------------------------ state

    /** Sets the name shown with the art (fitted to the banner, letters the font lacks are dropped) and the text shown without it. */
    public void setNames(String artName, String plainName) {
        if (!artName.equals(this.artName)) {
            this.artName = artName;
            art.setName(title(artName, tier));
        }
        if (!plainName.equals(this.plainName)) {
            this.plainName = plainName;
            plain.setName(Component.literal(plainName));
        }
    }

    /** 0 to 4 glowing tier lights. Rebuilds the title only when the value changes. */
    public void setTier(int tier) {
        int clamped = Math.max(0, Math.min(4, tier));
        if (clamped == this.tier) return;
        this.tier = clamped;
        art.setName(title(artName, clamped));
    }

    /** Progress 0..1; NaN and out-of-range values are clamped. The boss bar itself sends a packet only when the value changes. */
    public void setProgress(float value) {
        float p = Float.isNaN(value) ? 0f : Math.max(0f, Math.min(1f, value));
        art.setProgress(p);
        plain.setProgress(p);
    }

    /** Convenience for boss health: a zero, negative or non-finite maximum shows an empty bar instead of dividing by it. */
    public void setHealth(float current, float max) {
        setProgress(max > 0f && Float.isFinite(max) ? current / max : 0f);
    }

    private Component title(String name, int tier) {
        return Component.literal(layout.title(name, tier)).withStyle(Style.EMPTY.withFont(FONT).withColor(0xFFFFFF));
    }

    // ------------------------------------------------------------------------------------------------ viewers

    /** Shows the bar to a player; safe to call every tick and for a player who already has it. */
    public void addViewer(ServerPlayer player) {
        Mode wanted = modeFor(player);
        Mode current = viewers.get(player.getUUID());
        if (current == wanted) return;
        if (current != null) detach(player, current);
        attach(player, wanted);
        viewers.put(player.getUUID(), wanted);
    }

    public void removeViewer(ServerPlayer player) {
        Mode current = viewers.remove(player.getUUID());
        if (current != null) detach(player, current);
    }

    /** Re-sends the bar (remove, then add) for a viewer, in the mode that now applies. Used after respawn or a dimension change. */
    public void resync(ServerPlayer player) {
        Mode current = viewers.get(player.getUUID());
        if (current == null) return;
        detach(player, current);
        Mode wanted = modeFor(player);
        attach(player, wanted);
        viewers.put(player.getUUID(), wanted);
    }

    public boolean hasViewer(ServerPlayer player) {
        return viewers.containsKey(player.getUUID());
    }

    public Mode modeOf(ServerPlayer player) {
        return viewers.get(player.getUUID());
    }

    public int viewerCount() {
        return viewers.size();
    }

    /** Removes everyone and forgets the bar. Call when the encounter ends or the server stops. */
    public void close() {
        spacer.removeAllPlayers();
        tail.removeAllPlayers();
        art.removeAllPlayers();
        plain.removeAllPlayers();
        viewers.clear();
        synchronized (LIVE) { LIVE.remove(this); }
    }

    private void attach(ServerPlayer player, Mode mode) {
        if (mode == Mode.ART) {
            spacer.addPlayer(player);        // order matters: the client stacks bars in the order it received them
            art.addPlayer(player);
            tail.addPlayer(player);
        } else {
            plain.addPlayer(player);
        }
    }

    private void detach(ServerPlayer player, Mode mode) {
        if (mode == Mode.ART) {
            tail.removePlayer(player);
            art.removePlayer(player);
            spacer.removePlayer(player);
        } else {
            plain.removePlayer(player);
        }
    }

    private static Mode modeFor(ServerPlayer player) {
        return switch (policy) {
            case ART -> Mode.ART;
            case PLAIN -> Mode.PLAIN;
            case AUTO -> PolymerResourcePackUtils.hasMainPack(player) ? Mode.ART : Mode.PLAIN;
        };
    }

    // ------------------------------------------------------------------------------------------------ global hooks

    public static void setPolicy(Policy value) {
        policy = value == null ? Policy.AUTO : value;
    }

    private static List<RevenantBar> snapshot() {
        synchronized (LIVE) { return new ArrayList<>(LIVE); }
    }

    /** Registers the listeners that keep every live bar correct: pack status, disconnect, respawn, dimension change, shutdown. */
    public static synchronized void registerHooks() {
        if (hooked) return;
        hooked = true;
        // the pack finished loading (or was dropped) while the player was already in game: switch between PLAIN and ART
        PolymerCommonUtils.ON_RESOURCE_PACK_STATUS_CHANGE.register((handler, uuid, oldStatus, newStatus) -> {
            if (!(handler instanceof ServerGamePacketListenerImpl game) || !uuid.equals(PolymerResourcePackUtils.getMainUuid())) return;
            ServerPlayer player = game.player;
            player.getServer().execute(() -> guard(() -> {
                for (RevenantBar bar : snapshot()) bar.resync(player);
            }));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> guard(() -> {
            for (RevenantBar bar : snapshot()) bar.removeViewer(handler.getPlayer());
        }));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> guard(() -> {
            for (RevenantBar bar : snapshot()) {
                Mode mode = bar.viewers.remove(oldPlayer.getUUID());
                if (mode == null) continue;
                bar.detach(oldPlayer, mode);
                bar.addViewer(newPlayer);
            }
        }));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> guard(() -> {
            for (RevenantBar bar : snapshot()) bar.resync(player);
        }));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> guard(() -> {
            for (RevenantBar bar : snapshot()) bar.close();
        }));
    }

    private static void guard(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            SpookySurge.LOG.error("Revenant bar: update failed", e);
        }
    }
}
