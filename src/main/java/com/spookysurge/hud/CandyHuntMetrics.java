package com.spookysurge.hud;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Glyph code points and advances of the CandyHunt font, as written by {@code tools/bossbar-art/build_assets.py}. The server needs the
 * advances because it positions every glyph with space characters: it never asks the client how wide anything is.
 */
public final class CandyHuntMetrics {
    public static final String RESOURCE = "assets/spookysurge/hud/candyhunt_metrics.json";

    public static final class Glyph {
        public int cp;
        public int adv;
    }

    public static final class Frame {
        public int cpLeft;
        public int cpRight;
        public int advLeft;
        public int advRight;
        public int split;
        public int width;
        public int height;
    }

    public static final class Tier {
        public int cp;
        public int adv;
        public int[] lefts;
    }

    public static final class Plate {
        public int x0;
        public int x1;
    }

    public static final class Window {
        public int x;
        public int y;
        public int w;
        public int h;
    }

    public static final class Timer {
        public int right;
        public Map<String, Glyph> glyphs;
    }

    public Timer timer;
    public Frame frame;
    public Window window;
    @SerializedName("tier_lit")
    public Tier tierLit;
    public Plate plate;
    public int canvasCenter;
    public int titleWidth;
    public Map<String, Glyph> letters;

    private static CandyHuntMetrics cached;

    public static synchronized CandyHuntMetrics load() {
        if (cached == null) {
            try (InputStream in = CandyHuntMetrics.class.getClassLoader().getResourceAsStream(RESOURCE)) {
                if (in == null) throw new IllegalStateException("missing " + RESOURCE);
                cached = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), CandyHuntMetrics.class);
            } catch (IOException e) {
                throw new IllegalStateException("cannot read " + RESOURCE, e);
            }
        }
        return cached;
    }

    /** The glyph for a (already sanitised) character, or null when the font has none. */
    public Glyph glyph(char c) {
        return letters.get(String.valueOf(c));
    }
}
