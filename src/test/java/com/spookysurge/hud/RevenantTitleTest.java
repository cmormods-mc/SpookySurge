package com.spookysurge.hud;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RevenantTitleTest {
    private final RevenantMetrics m = RevenantMetrics.load();
    private final RevenantTitle t = new RevenantTitle(m);

    /** Where each non-space glyph starts, replaying the advances the way the client does. */
    private record Placed(int cp, int x) {}

    private List<Placed> replay(String text) {
        List<Placed> out = new ArrayList<>();
        int x = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int before = x;
            x = new RevenantTitle(m).advanceOf(String.valueOf(c)) + x;
            if (!(c >= '' && c <= '')) out.add(new Placed(c, before));
        }
        return out;
    }

    @Test
    void everyTitleIsExactlyTheWindowWidth() {
        String[] names = {"", "THE CANDY CORN REVENANT", "Spooky Surge 1234/5000", "a", "SURGE BEGINS IN 45S", "x".repeat(80),
                "Pokémon  Trainer", "中文 name", "W".repeat(30)};
        for (String name : names) for (int tier = 0; tier <= 4; tier++) {
            String title = t.title(name, tier);
            assertEquals(m.titleWidth, t.advanceOf(title), "name '" + name + "' tier " + tier);
            assertEquals(0, m.titleWidth % 2, "an even width keeps the frame on the bar whatever the GUI width's parity");
        }
    }

    @Test
    void frameIsFirstAndSitsAtZero() {
        List<Placed> p = replay(t.title("A", 0));
        assertEquals(m.frame.cpLeft, p.get(0).cp());
        assertEquals(m.frame.cpRight, p.get(1).cp());
        assertEquals(m.frame.split, p.get(1).x(), "the halves meet on the seam");
        assertEquals(0, p.get(0).x());
    }

    @Test
    void windowIsCentredOnTheTitle() {
        // The client puts the title's left edge at centre - titleWidth/2 and the bar's left edge at centre - 91, so the frame's window
        // (the hole over the bar) must start exactly 91 px before the title's middle.
        assertEquals(91, m.titleWidth / 2 - m.window.x);
        assertEquals(182, m.window.w);
        assertEquals(5, m.window.h);
        assertEquals(262, m.titleWidth);
    }

    @Test
    void nameIsCentredOnThePlate() {
        for (String name : new String[] {"THE CANDY CORN REVENANT", "SURGE", "A", "SPOOKY SURGE 12/300"}) {
            String run = t.fit(t.sanitize(name));
            List<Placed> p = replay(t.title(name, 0));
            int first = p.get(2).x();
            int last = p.get(p.size() - 1).x() + m.letters.values().stream().filter(g -> g.cp == p.get(p.size() - 1).cp()).findFirst().get().adv - 1;
            int centre = (first + last) / 2;
            assertTrue(Math.abs(centre - m.canvasCenter) <= 1, name + " centre " + centre + " vs " + m.canvasCenter + " for " + run);
            assertTrue(first >= m.plate.x0 && last <= m.plate.x1, name + " stays inside the plate: " + first + ".." + last);
        }
    }

    @Test
    void tierLightsAppearOverTheFramedStuds() {
        for (int tier = 0; tier <= 4; tier++) {
            List<Placed> lit = replay(t.title("X", tier)).stream().filter(g -> g.cp() == m.tierLit.cp).toList();
            assertEquals(tier, lit.size());
            for (int i = 0; i < tier; i++) assertEquals(m.tierLit.lefts[i], lit.get(i).x());
        }
        assertEquals(4, replay(t.title("X", 99)).stream().filter(g -> g.cp() == m.tierLit.cp).count(), "tier above 4 is clamped");
        assertEquals(0, replay(t.title("X", -3)).stream().filter(g -> g.cp() == m.tierLit.cp).count(), "negative tier is clamped");
    }

    @Test
    void sanitizeKeepsOnlyWhatTheFontHas() {
        assertEquals("POKEMON TRAINER", t.sanitize("  Pokémon \t Trainer  "));
        assertEquals("A-B", t.sanitize("a☃-b"));
        assertEquals("", t.sanitize(null));
        assertEquals("", t.sanitize("☃☃"));
        assertEquals("AB CD", t.sanitize("ab_cd".replace('_', ' ')));
    }

    @Test
    void longNamesAreCutWithEllipsisAndFit() {
        String fitted = t.fit(t.sanitize("THE VERY LONG NAME OF A BOSS THAT NEVER ENDS AND KEEPS GOING"));
        assertTrue(fitted.endsWith("..."));
        assertTrue(t.visualWidth(fitted) <= m.plate.x1 - m.plate.x0);
        assertEquals("SHORT", t.fit("SHORT"));
    }

    @Test
    void demoNameFitsWithoutCutting() {
        String run = t.sanitize("THE CANDY CORN REVENANT");
        assertEquals(run, t.fit(run));
    }

    @Test
    void spacesAreExactInBothDirections() {
        RevenantTitle probe = new RevenantTitle(m);
        for (int n = -400; n <= 400; n++) assertEquals(n, probe.advanceOf(RevenantTitle.spaces(n)), "spaces(" + n + ")");
    }
}
