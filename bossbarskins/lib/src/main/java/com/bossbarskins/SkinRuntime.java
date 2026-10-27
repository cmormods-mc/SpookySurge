package com.bossbarskins;

import com.bossbarskins.api.OwnerHolder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.BossEvent;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;

/** The lookup the packet mixins use: which colour and overlay to put on the wire for a boss event. Lock-free: the table is replaced whole. */
public final class SkinRuntime {
    private SkinRuntime() {}

    private static volatile Map<String, Slot> byRequest = Map.of();

    /** Installs the table built from the allocation: (owner, requested slot) -> assigned slot. */
    static void install(Map<String, Slot> assignedBySkinKey, Map<String, Skin> skinsByKey) {
        Map<String, Slot> table = new HashMap<>();
        for (Map.Entry<String, Slot> e : assignedBySkinKey.entrySet()) {
            Skin s = skinsByKey.get(e.getKey());
            table.put(s.requested().requestKey(s.owner()), e.getValue());
        }
        byRequest = Map.copyOf(table);
    }

    /** The slot a bar should be drawn with, or null to leave it exactly as its mod asked. */
    public static Slot slotFor(BossEvent event) {
        Map<String, Slot> table = byRequest;
        if (table.isEmpty()) return null;
        String owner = ((OwnerHolder) event).bossbarskins$getOwner();
        if (owner == null) return null;
        return table.get(new Slot(event.getColor(), event.getOverlay()).requestKey(owner));
    }

    public static BossBarColor color(BossEvent event) {
        Slot slot = slotFor(event);
        return slot == null ? event.getColor() : slot.color();
    }

    public static BossBarOverlay overlay(BossEvent event) {
        Slot slot = slotFor(event);
        return slot == null ? event.getOverlay() : slot.overlay();
    }
}
