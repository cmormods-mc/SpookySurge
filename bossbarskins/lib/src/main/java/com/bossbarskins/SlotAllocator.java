package com.bossbarskins;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;

/**
 * Decides which slot each skin gets. Pure logic, no Minecraft state, so it is unit tested.
 *
 * <p>Rules, in order, for each skin (sorted by key so the result never depends on mod load order):
 * <ol>
 *   <li>a pin from the config, if that slot is free;</li>
 *   <li>the slot assigned last time (kept from the saved assignments), if it is still free: adding a mod never reshuffles the others;</li>
 *   <li>the slot the mod's code asked for, if it is free: a mod alone on a colour sees no remapping at all;</li>
 *   <li>otherwise a free slot from the pool: colours first, then notched overlays (only for skins whose sprites are fully opaque, because
 *       an overlay is drawn over the colour's sprites and must hide them);</li>
 *   <li>otherwise the skin gets nothing, a problem is reported, and that mod's bars stay vanilla.</li>
 * </ol>
 * All transparent skins share one slot. Slots in {@code reserved} (by default the colours vanilla's own bosses use) are never handed out
 * by rule 4, but a mod may still ask for one explicitly. The defaults also reserve NOTCHED_20, which CobbleRaids' World Boss bar already
 * uses through AscensionLib's static pack.
 */
public final class SlotAllocator {
    private SlotAllocator() {}

    public record Result(Map<String, Slot> assigned, Slot transparentSlot, List<String> problems) {}

    public static List<Slot> defaultPool() {
        List<Slot> pool = new ArrayList<>();
        for (BossBarColor c : new BossBarColor[] {BossBarColor.YELLOW, BossBarColor.GREEN, BossBarColor.WHITE, BossBarColor.BLUE}) pool.add(Slot.colour(c));
        for (BossBarOverlay o : new BossBarOverlay[] {BossBarOverlay.NOTCHED_6, BossBarOverlay.NOTCHED_10, BossBarOverlay.NOTCHED_12, BossBarOverlay.NOTCHED_20})
            pool.add(new Slot(BossBarColor.WHITE, o));
        return pool;
    }

    public static Set<String> defaultReserved() {
        return Set.of("pink", "purple", "red", "notched_20");
    }

    public static Result allocate(List<Skin> skins, Map<String, Slot> previous, Map<String, Slot> pins, Set<String> reserved, List<Slot> pool) {
        Map<String, Slot> assigned = new LinkedHashMap<>();
        Map<String, String> occupied = new HashMap<>();
        List<String> problems = new ArrayList<>();
        List<Skin> sorted = new ArrayList<>(skins);
        sorted.sort(Comparator.comparing(Skin::key));

        Slot transparent = null;
        for (Skin s : sorted) {
            if (!s.transparent()) continue;
            if (transparent == null) {
                Slot want = pins.getOrDefault(s.key(), previous.getOrDefault(s.key(), s.requested()));
                transparent = Slot.colour(want.color());
                occupied.put(transparent.identity(), "(transparent)");
            }
            assigned.put(s.key(), transparent);
        }
        // pass 1: pins, pass 2: what was assigned last time, pass 3: the requested slot, pass 4: anything free from the pool.
        // Whole passes, not per skin, so a skin kept from last time is never pushed out by one that happens to sort earlier.
        for (int pass = 1; pass <= 4; pass++) {
            for (Skin s : sorted) {
                if (s.transparent() || assigned.containsKey(s.key())) continue;
                Slot slot = firstFree(candidates(s, previous, pins, reserved, pool, pass), s, occupied);
                if (slot == null) continue;
                occupied.put(slot.identity(), s.key());
                assigned.put(s.key(), slot);
                if (pass == 4) problems.add(s.key() + " asked for " + s.requested() + " but it is taken; it was given " + slot + " instead");
            }
        }
        for (Skin s : sorted) {
            if (!s.transparent() && !assigned.containsKey(s.key()))
                problems.add(s.key() + " found no free boss bar slot (asked for " + s.requested() + "); its bars stay vanilla. Free a slot or remove a skin.");
        }
        return new Result(assigned, transparent, problems);
    }

    private static List<Slot> candidates(Skin s, Map<String, Slot> previous, Map<String, Slot> pins, Set<String> reserved, List<Slot> pool, int pass) {
        List<Slot> out = new ArrayList<>();
        switch (pass) {
            case 1 -> { if (pins.containsKey(s.key())) out.add(pins.get(s.key())); }
            case 2 -> { if (previous.containsKey(s.key())) out.add(previous.get(s.key())); }
            case 3 -> out.add(s.requested());
            default -> {
                for (Slot p : pool) if (!reserved.contains(p.identity()) && p.overlay() == BossBarOverlay.PROGRESS) out.add(p);
                for (Slot p : pool) if (!reserved.contains(p.identity()) && p.overlay() != BossBarOverlay.PROGRESS) out.add(new Slot(s.requested().color(), p.overlay()));
            }
        }
        return out;
    }

    private static Slot firstFree(List<Slot> candidates, Skin s, Map<String, String> occupied) {
        Set<String> tried = new HashSet<>();
        for (Slot c : candidates) {
            if (!tried.add(c.toString())) continue;
            if (occupied.containsKey(c.identity())) continue;
            if (c.overlay() != BossBarOverlay.PROGRESS && !s.opaque()) continue;       // an overlay must be opaque to hide the colour under it
            return c;
        }
        return null;
    }
}
