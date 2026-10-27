package com.bossbarskins;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import org.junit.jupiter.api.Test;

class SlotAllocatorTest {
    private static Skin skin(String owner, String id, String match, boolean opaque) {
        return new Skin(owner, id, Slot.parse(match), false, opaque, "a.png", "b.png", null);
    }

    private static Skin spacer(String owner, String match) {
        return new Skin(owner, "spacer", Slot.parse(match), true, false, null, null, null);
    }

    private SlotAllocator.Result run(List<Skin> skins) {
        return SlotAllocator.allocate(skins, Map.of(), Map.of(), SlotAllocator.defaultReserved(), SlotAllocator.defaultPool());
    }

    @Test
    void aModAloneOnAColourIsNotRemapped() {
        SlotAllocator.Result r = run(List.of(skin("a", "bar", "YELLOW", true)));
        assertEquals(Slot.parse("YELLOW"), r.assigned().get("a:bar"));
        assertTrue(r.problems().isEmpty());
    }

    @Test
    void twoModsOnTheSameColourGetDifferentSlotsAndTheFirstByKeyKeepsIt() {
        SlotAllocator.Result r = run(List.of(skin("zeta", "bar", "YELLOW", true), skin("alpha", "bar", "YELLOW", true)));
        assertEquals(Slot.parse("YELLOW"), r.assigned().get("alpha:bar"));
        assertEquals(Slot.parse("GREEN"), r.assigned().get("zeta:bar"));
        assertEquals(1, r.problems().size(), "the relocation is reported");
    }

    @Test
    void resultDoesNotDependOnInputOrder() {
        List<Skin> a = List.of(skin("a", "x", "YELLOW", true), skin("b", "x", "YELLOW", true), skin("c", "x", "YELLOW", true));
        List<Skin> b = List.of(a.get(2), a.get(0), a.get(1));
        assertEquals(run(a).assigned(), run(b).assigned());
    }

    @Test
    void relocationNeverLandsOnVanillaBossColours() {
        List<Skin> skins = List.of(skin("a", "x", "YELLOW", true), skin("b", "x", "YELLOW", true), skin("c", "x", "YELLOW", true),
                skin("d", "x", "YELLOW", true), skin("e", "x", "YELLOW", true));
        for (Slot s : run(skins).assigned().values()) {
            assertFalse(Set.of("pink", "purple", "red", "notched_20").contains(s.identity()), s + " is reserved");
        }
    }

    @Test
    void opaqueSkinsSpillIntoNotchedOverlaysWhenColoursRunOut() {
        // pool colours: yellow, green, white, blue = 4 slots, then 3 overlays (notched_20 is reserved by default)
        List<Skin> skins = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) skins.add(skin("m" + i, "x", "YELLOW", true));
        SlotAllocator.Result r = run(skins);
        assertEquals(7, r.assigned().size());
        long overlays = r.assigned().values().stream().filter(s -> s.overlay() != BossBarOverlay.PROGRESS).count();
        assertEquals(3, overlays);
        assertEquals(7, r.assigned().values().stream().map(Slot::identity).distinct().count(), "every skin has its own identity");
    }

    @Test
    void nonOpaqueSkinsNeverGoToAnOverlaySlot() {
        List<Skin> skins = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) skins.add(skin("m" + i, "x", "YELLOW", i != 5));
        SlotAllocator.Result r = run(skins);
        assertNull(r.assigned().get("m5:x"), "the translucent skin has no colour left and cannot use an overlay");
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("m5:x") && p.contains("no free")));
    }

    @Test
    void allTransparentRequestsShareOneSlot() {
        SlotAllocator.Result r = run(List.of(spacer("a", "BLUE"), spacer("b", "BLUE"), skin("a", "bar", "YELLOW", true)));
        assertEquals(r.assigned().get("a:spacer"), r.assigned().get("b:spacer"));
        assertEquals(Slot.parse("BLUE"), r.transparentSlot());
        assertNotEquals(r.transparentSlot().identity(), r.assigned().get("a:bar").identity());
    }

    @Test
    void aSkinAskingForTheTransparentSlotIsMovedAway() {
        SlotAllocator.Result r = run(List.of(spacer("a", "BLUE"), skin("b", "bar", "BLUE", true)));
        assertNotEquals("blue", r.assigned().get("b:bar").identity());
    }

    @Test
    void savedAssignmentsAreKeptSoAddingAModDoesNotReshuffle() {
        Map<String, Slot> saved = Map.of("zeta:bar", Slot.parse("GREEN"));
        SlotAllocator.Result r = SlotAllocator.allocate(List.of(skin("alpha", "bar", "YELLOW", true), skin("zeta", "bar", "YELLOW", true),
                skin("mid", "bar", "YELLOW", true)), saved, Map.of(), SlotAllocator.defaultReserved(), SlotAllocator.defaultPool());
        assertEquals(Slot.parse("GREEN"), r.assigned().get("zeta:bar"));
        assertEquals(Slot.parse("YELLOW"), r.assigned().get("alpha:bar"));
        assertEquals(Slot.parse("WHITE"), r.assigned().get("mid:bar"));
    }

    @Test
    void pinsWinWhileTheSlotIsFree() {
        SlotAllocator.Result r = SlotAllocator.allocate(List.of(skin("a", "bar", "YELLOW", true)), Map.of(), Map.of("a:bar", Slot.parse("WHITE")),
                SlotAllocator.defaultReserved(), SlotAllocator.defaultPool());
        assertEquals(Slot.parse("WHITE"), r.assigned().get("a:bar"));
    }

    @Test
    void aModMayExplicitlyAskForAVanillaBossColour() {
        SlotAllocator.Result r = run(List.of(skin("ascension", "world_boss", "PINK", true)));
        assertEquals(Slot.parse("PINK"), r.assigned().get("ascension:world_boss"));
    }

    @Test
    void overlayIdentityIgnoresColour() {
        assertEquals(new Slot(BossBarColor.RED, BossBarOverlay.NOTCHED_6).identity(), new Slot(BossBarColor.GREEN, BossBarOverlay.NOTCHED_6).identity());
        assertNotEquals(Slot.colour(BossBarColor.RED).identity(), Slot.colour(BossBarColor.GREEN).identity());
    }
}
