package com.spookysurge.hud;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Builds the boss-bar title string for the Spooky Surge Candy Hunt: the two frame glyphs, the glowing tier lights and the boss name set in the
 * pack's own pixel font, all placed with {@code space} characters.
 *
 * <p>The client centres a bar's title on the bar. Every glyph and space here has a known advance, so the title is exactly
 * {@link CandyHuntMetrics#titleWidth} wide whatever the name is, which keeps the frame's 182x5 window on top of the vanilla bar at every
 * GUI scale and every window width. Pure string work: no Minecraft classes, so it can be unit tested.
 */
public final class CandyHuntTitle {
    private static final char SPACE_BASE_POS = '';
    private static final char SPACE_BASE_NEG = '';

    private final CandyHuntMetrics m;

    public CandyHuntTitle(CandyHuntMetrics metrics) {
        this.m = metrics;
    }

    /** Upper-cases, strips accents, keeps only characters the font has, collapses blanks. Everything else is dropped. */
    public String sanitize(String name) {
        if (name == null) return "";
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toUpperCase(Locale.ROOT);
        StringBuilder out = new StringBuilder();
        boolean pendingSpace = false;
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (Character.isWhitespace(c) || c == '_') {
                pendingSpace = out.length() > 0;
            } else if (m.glyph(c) != null) {
                if (pendingSpace) out.append(' ');
                pendingSpace = false;
                out.append(c);
            }
        }
        return out.toString();
    }

    /** Width of a sanitised run as it appears on screen (the last glyph's trailing 1 px is not ink). */
    public int visualWidth(String run) {
        return run.isEmpty() ? 0 : advance(run) - 1;
    }

    private int advance(String run) {
        int total = 0;
        for (int i = 0; i < run.length(); i++) total += m.glyph(run.charAt(i)).adv;
        return total;
    }

    /** Fits a sanitised name into the banner plate, ending in "..." when it has to be cut. */
    public String fit(String run) {
        int max = m.plate.x1 - m.plate.x0;
        if (visualWidth(run) <= max) return run;
        String dots = "...";
        String cut = run;
        while (!cut.isEmpty() && visualWidth(cut.stripTrailing() + dots) > max) cut = cut.substring(0, cut.length() - 1);
        return cut.stripTrailing() + dots;
    }

    /** The title without a timer. */
    public String title(String name, int tier) {
        return title(name, tier, null);
    }

    /** Keeps only digits and colons, at most 8 characters. */
    public String sanitizeTimer(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length() && out.length() < 8; i++) if (m.timer.glyphs.containsKey(String.valueOf(text.charAt(i)))) out.append(text.charAt(i));
        return out.toString();
    }

    /** The full title: frame, lit tier lights (0 to 4), the centred name, and an optional small timer under the bar (e.g. "27:00"). */
    public String title(String name, int tier, String timer) {
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        out.appendCodePoint(m.frame.cpLeft);
        cursor += m.frame.advLeft;
        cursor = moveTo(out, cursor, m.frame.split);       // the left half's advance includes 1 px of trailing space: step back onto the seam
        out.appendCodePoint(m.frame.cpRight);
        cursor += m.frame.advRight;
        int lit = Math.max(0, Math.min(m.tierLit.lefts.length, tier));
        for (int i = 0; i < lit; i++) {
            cursor = moveTo(out, cursor, m.tierLit.lefts[i]);
            out.appendCodePoint(m.tierLit.cp);
            cursor += m.tierLit.adv;
        }
        String run = fit(sanitize(name));
        if (!run.isEmpty()) {
            cursor = moveTo(out, cursor, m.canvasCenter - visualWidth(run) / 2);
            for (int i = 0; i < run.length(); i++) {
                CandyHuntMetrics.Glyph g = m.glyph(run.charAt(i));
                out.appendCodePoint(g.cp);
                cursor += g.adv;
            }
        }
        String t = sanitizeTimer(timer);
        if (!t.isEmpty()) {
            int width = -1;
            for (int i = 0; i < t.length(); i++) width += m.timer.glyphs.get(String.valueOf(t.charAt(i))).adv;
            cursor = moveTo(out, cursor, m.timer.right - width);
            for (int i = 0; i < t.length(); i++) {
                CandyHuntMetrics.Glyph g = m.timer.glyphs.get(String.valueOf(t.charAt(i)));
                out.appendCodePoint(g.cp);
                cursor += g.adv;
            }
        }
        moveTo(out, cursor, m.titleWidth);
        return out.toString();
    }

    private int moveTo(StringBuilder out, int cursor, int target) {
        out.append(spaces(target - cursor));
        return target;
    }

    /** Space characters summing to {@code n} (negative moves back); the font defines +-1, 2, 4 ... 128. */
    static String spaces(int n) {
        StringBuilder sb = new StringBuilder();
        char base = n >= 0 ? SPACE_BASE_POS : SPACE_BASE_NEG;
        int a = Math.abs(n);
        for (int bit = 7; bit >= 0; bit--) {
            int v = 1 << bit;
            while (a >= v) {
                sb.append((char) (base + v));
                a -= v;
            }
        }
        return sb.toString();
    }

    /** Total advance of a title string: what the client sums to centre it. Used by tests and the self-check. */
    public int advanceOf(String text) {
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == m.frame.cpLeft) total += m.frame.advLeft;
            else if (c == m.frame.cpRight) total += m.frame.advRight;
            else if (c == m.tierLit.cp) total += m.tierLit.adv;
            else if (c >= SPACE_BASE_POS && c <= SPACE_BASE_POS + 128) total += c - SPACE_BASE_POS;
            else if (c >= SPACE_BASE_NEG && c <= SPACE_BASE_NEG + 128) total -= c - SPACE_BASE_NEG;
            else total += glyphAdvance(c);
        }
        return total;
    }

    private int glyphAdvance(char c) {
        for (CandyHuntMetrics.Glyph g : m.letters.values()) if (g.cp == c) return g.adv;
        for (CandyHuntMetrics.Glyph g : m.timer.glyphs.values()) if (g.cp == c) return g.adv;
        throw new IllegalArgumentException("unknown glyph U+" + Integer.toHexString(c));
    }
}
