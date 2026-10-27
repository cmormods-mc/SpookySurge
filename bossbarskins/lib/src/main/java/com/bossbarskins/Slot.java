package com.bossbarskins;

import java.util.Locale;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;

/**
 * One thing the client can be asked to draw: a boss bar colour with the plain PROGRESS overlay, or a notched overlay (which, when its
 * sprites are opaque, completely covers whatever the colour drew, so it works as a skin of its own). 1.21.1 chooses sprites by these two
 * enums only, so these are all the distinct looks a server can ask for; there is no per-bar texture id.
 */
public record Slot(BossBarColor color, BossBarOverlay overlay) {
    /** What the slot's sprites are named after: the colour for a PROGRESS slot, otherwise the overlay (the colour does not matter then). */
    public String identity() {
        return (overlay == BossBarOverlay.PROGRESS ? color : overlay).name().toLowerCase(Locale.ROOT);
    }

    public static Slot colour(BossBarColor color) {
        return new Slot(color, BossBarOverlay.PROGRESS);
    }

    /** "YELLOW" or "YELLOW/NOTCHED_6". */
    public static Slot parse(String text) {
        String[] parts = text.trim().toUpperCase(Locale.ROOT).split("/");
        BossBarColor color = BossBarColor.valueOf(parts[0].trim());
        BossBarOverlay overlay = parts.length > 1 ? BossBarOverlay.valueOf(parts[1].trim()) : BossBarOverlay.PROGRESS;
        return new Slot(color, overlay);
    }

    @Override
    public String toString() {
        return overlay == BossBarOverlay.PROGRESS ? color.name() : color.name() + "/" + overlay.name();
    }

    public String requestKey(String owner) {
        return owner + "|" + color.name() + "|" + overlay.name();
    }
}
