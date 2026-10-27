package com.spookysurge.hud;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Static checks of the shipped art: sizes, alpha, the font's references and the metrics the server lays text out with. */
class CandyHuntAssetsTest {
    private static final String SPRITES = "bossbarskins/candyhunt/";
    private static final String FONT_TEX = "assets/spookysurge/textures/font/candyhunt/";
    private final CandyHuntMetrics m = CandyHuntMetrics.load();

    private static InputStream open(String path) {
        InputStream in = CandyHuntAssetsTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(in, "missing from the jar: " + path);
        return in;
    }

    private static BufferedImage png(String path) throws IOException {
        try (InputStream in = open(path)) { return ImageIO.read(in); }
    }

    private static int alpha(BufferedImage im, int x, int y) { return im.getRGB(x, y) >>> 24; }

    /** The two frame glyphs laid side by side as they appear on screen. */
    private BufferedImage frame() throws IOException {
        BufferedImage l = png(FONT_TEX + "frame_l.png"), r = png(FONT_TEX + "frame_r.png");
        BufferedImage whole = new BufferedImage(l.getWidth() + r.getWidth(), l.getHeight(), BufferedImage.TYPE_INT_ARGB);
        whole.getGraphics().drawImage(l, 0, 0, null);
        whole.getGraphics().drawImage(r, l.getWidth(), 0, null);
        return whole;
    }

    private static JsonObject json(String path) throws IOException {
        try (InputStream in = open(path)) { return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject(); }
    }

    @Test
    void everyFileTheBuilderAddsExists() throws IOException {
        for (String f : CandyHuntPack.FILES) open(f).transferTo(java.io.OutputStream.nullOutputStream());
    }

    @Test
    void yellowSpritesHaveTheExactBarSizeAndAreOpaque() throws IOException {
        BufferedImage bg = png(SPRITES + "background.png");
        assertEquals(182, bg.getWidth());
        assertEquals(5, bg.getHeight());
        for (int y = 0; y < 5; y++) for (int x = 0; x < 182; x++) assertEquals(255, alpha(bg, x, y), "background pixel " + x + "," + y);
        BufferedImage prog = png(SPRITES + "progress.png");
        assertEquals(182, prog.getWidth());
        assertEquals(0, prog.getHeight() % 5);
        for (int y = 0; y < prog.getHeight(); y++) for (int x = 0; x < 182; x++) assertEquals(255, alpha(prog, x, y), "progress pixel " + x + "," + y);
        JsonObject anim = json(SPRITES + "progress.png.mcmeta").getAsJsonObject("animation");
        assertEquals(182, anim.get("width").getAsInt());
        assertEquals(5, anim.get("height").getAsInt());
        assertTrue(anim.get("frametime").getAsInt() >= 1);
        assertTrue(prog.getHeight() / 5 >= 2, "the fill is animated");
    }

    @Test
    void emptyAndFillAreClearlyDifferent() throws IOException {
        BufferedImage bg = png(SPRITES + "background.png"), prog = png(SPRITES + "progress.png");
        double bgLum = 0, fillLum = 0;
        for (int y = 1; y < 4; y++) for (int x = 0; x < 182; x++) { bgLum += lum(bg.getRGB(x, y)); fillLum += lum(prog.getRGB(x, y)); }
        bgLum /= 3 * 182; fillLum /= 3 * 182;
        assertTrue(fillLum > bgLum * 3.5, "fill luminance " + fillLum + " vs cavity " + bgLum);
    }

    private static double lum(int argb) {
        return 0.2126 * ((argb >> 16) & 255) + 0.7152 * ((argb >> 8) & 255) + 0.0722 * (argb & 255);
    }

    @Test
    void blueSpacerIsInvisible() throws IOException {
        for (String n : new String[] {"transparent.png"}) {
            BufferedImage im = png(SPRITES + n);
            assertEquals(182, im.getWidth());
            assertEquals(5, im.getHeight());
            for (int y = 0; y < 5; y++) for (int x = 0; x < 182; x++) assertEquals(0, alpha(im, x, y), n + " " + x + "," + y);
        }
    }

    @Test
    void frameWindowIsTransparentSoTheNativeBarShowsThrough() throws IOException {
        BufferedImage f = frame();
        assertEquals(m.frame.width, f.getWidth());
        assertEquals(m.frame.split, png(FONT_TEX + "frame_l.png").getWidth());
        assertEquals(56, f.getHeight());
        for (int y = m.window.y; y < m.window.y + m.window.h; y++)
            for (int x = m.window.x; x < m.window.x + m.window.w; x++) assertEquals(0, alpha(f, x, y), "window pixel " + x + "," + y);
        assertEquals(0, alpha(f, 0, 0), "no opaque background in the corners");
        assertEquals(0, alpha(f, 100, 0));
        int clear = 0;
        for (int y = 0; y < f.getHeight(); y++) for (int x = 0; x < f.getWidth(); x++) if (alpha(f, x, y) == 0) clear++;
        assertTrue(clear > f.getWidth() * f.getHeight() * 0.35, "mostly transparent canvas");
        assertTrue(hasInkInLastColumn(png(FONT_TEX + "frame_r.png")), "the right half's last column has ink, so its width is not trimmed");
        assertTrue(hasInkInLastColumn(png(FONT_TEX + "frame_l.png")), "the left half's last column has ink, so its width is not trimmed");
    }

    @Test
    void frameIsAsymmetricCandyCornFaceLeftClusterRight() throws IOException {
        BufferedImage f = frame();
        int leftInk = 0, rightInk = 0;
        for (int y = 0; y < f.getHeight(); y++) for (int x = 0; x < 45; x++) if (alpha(f, x, y) > 0) leftInk++;
        for (int y = 0; y < f.getHeight(); y++) for (int x = 224; x < 262; x++) if (alpha(f, x, y) > 0) rightInk++;
        assertTrue(leftInk > rightInk, "the face (left) is the larger ornament");
    }

    @Test
    void fontJsonIsConsistent() throws IOException {
        JsonArray providers = json("assets/spookysurge/font/candyhunt.json").getAsJsonArray("providers");
        Set<Character> defined = new HashSet<>();
        Set<Character> seen = new HashSet<>();
        for (JsonElement e : providers) {
            JsonObject p = e.getAsJsonObject();
            if (p.get("type").getAsString().equals("space")) {
                for (String k : p.getAsJsonObject("advances").keySet()) { assertEquals(1, k.length()); defined.add(k.charAt(0)); }
                continue;
            }
            assertEquals("bitmap", p.get("type").getAsString());
            String file = p.get("file").getAsString();
            assertTrue(file.startsWith("spookysurge:font/candyhunt/") && file.equals(file.toLowerCase()), "lowercase namespaced path: " + file);
            BufferedImage im = png("assets/spookysurge/textures/" + file.substring("spookysurge:".length()));
            assertTrue(im.getWidth() <= 256 && im.getHeight() <= 256,
                    file + " is " + im.getWidth() + "x" + im.getHeight() + ": 1.21.1 bakes glyphs into 256x256 atlas pages, a bigger one renders as a missing-glyph box");
            int ascent = p.get("ascent").getAsInt(), height = p.get("height").getAsInt();
            assertTrue(ascent <= height, file + ": Minecraft rejects the WHOLE font when ascent > height (" + ascent + " > " + height + ")");
            JsonArray rows = p.getAsJsonArray("chars");
            int cols = rows.get(0).getAsString().length();
            for (JsonElement r : rows) assertEquals(cols, r.getAsString().length(), "equal rows in " + file);
            assertEquals(0, im.getWidth() % cols);
            assertEquals(0, im.getHeight() % rows.size());
            assertEquals(height, im.getHeight() / rows.size(), file + ": height equals the cell height, so the art is drawn 1:1 (no resampling)");
            for (JsonElement r : rows) for (char c : r.getAsString().toCharArray()) if (c != 0) { assertTrue(seen.add(c), "duplicate glyph " + (int) c); defined.add(c); }
        }
        assertTrue(defined.contains((char) m.frame.cpLeft));
        assertTrue(defined.contains((char) m.frame.cpRight));
        assertTrue(defined.contains((char) m.tierLit.cp));
        for (CandyHuntMetrics.Glyph g : m.letters.values()) assertTrue(defined.contains((char) g.cp), "undefined U+" + Integer.toHexString(g.cp));
        for (int bit = 0; bit < 8; bit++) {
            assertTrue(defined.contains((char) (0xE200 + (1 << bit))));
            assertTrue(defined.contains((char) (0xE300 + (1 << bit))));
        }
        JsonObject space = null;
        for (JsonElement e : providers) if (e.getAsJsonObject().get("type").getAsString().equals("space")) space = e.getAsJsonObject().getAsJsonObject("advances");
        for (int bit = 0; bit < 8; bit++) {
            assertEquals(1 << bit, space.get(String.valueOf((char) (0xE200 + (1 << bit)))).getAsInt());
            assertEquals(-(1 << bit), space.get(String.valueOf((char) (0xE300 + (1 << bit)))).getAsInt());
        }
    }

    @Test
    void metricsMatchThePixels() throws IOException {
        // A bitmap glyph advances (its rightmost inked column + 1) + 1. Recompute that from the PNG and compare with what the server uses.
        BufferedImage letters = png(FONT_TEX + "letters.png");
        JsonArray rows = null;
        for (JsonElement e : json("assets/spookysurge/font/candyhunt.json").getAsJsonArray("providers")) {
            JsonObject p = e.getAsJsonObject();
            if (p.get("type").getAsString().equals("bitmap") && p.get("file").getAsString().endsWith("letters.png")) rows = p.getAsJsonArray("chars");
        }
        int cols = rows.get(0).getAsString().length(), cw = letters.getWidth() / cols, ch = letters.getHeight() / rows.size();
        int checked = 0;
        for (int r = 0; r < rows.size(); r++) for (int c = 0; c < cols; c++) {
            char g = rows.get(r).getAsString().charAt(c);
            if (g == 0) continue;
            int right = -1;
            for (int x = 0; x < cw; x++) for (int y = 0; y < ch; y++) if (alpha(letters, c * cw + x, r * ch + y) > 0) right = Math.max(right, x);
            final char glyphChar = g;
            CandyHuntMetrics.Glyph meta = m.letters.values().stream().filter(v -> v.cp == glyphChar).findFirst().orElseThrow();
            assertEquals(right + 2, meta.adv, "advance of U+" + Integer.toHexString(g));
            checked++;
        }
        assertEquals(m.letters.size() - 1, checked, "every letter except the space is in the sheet");
        BufferedImage timerSheet = png(FONT_TEX + "timer.png");
        int tw = timerSheet.getWidth() / 11;
        for (String tc : "0123456789:".split("")) {
            int index = "0123456789:".indexOf(tc), right = -1;
            for (int x = 0; x < tw; x++) for (int y = 0; y < timerSheet.getHeight(); y++) if (alpha(timerSheet, index * tw + x, y) > 0) right = Math.max(right, x);
            assertEquals(right + 2, m.timer.glyphs.get(tc).adv, "timer glyph " + tc);
        }
        BufferedImage stud = png(FONT_TEX + "tier_lit.png");
        assertEquals(png(FONT_TEX + "frame_l.png").getWidth() + 1, m.frame.advLeft);
        assertEquals(png(FONT_TEX + "frame_r.png").getWidth() + 1, m.frame.advRight);
        assertEquals(stud.getWidth() + 1, m.tierLit.adv);
        assertTrue(alpha(stud, stud.getWidth() - 1, stud.getHeight() - 1) > 0 || hasInkInLastColumn(stud), "tier light width is pinned");
    }

    private static boolean hasInkInLastColumn(BufferedImage im) {
        for (int y = 0; y < im.getHeight(); y++) if (alpha(im, im.getWidth() - 1, y) > 0) return true;
        return false;
    }

    @Test
    void skinDeclarationMatchesWhatTheBarAsksFor() throws IOException {
        JsonObject root = json("bossbarskins.json");
        boolean art = false, spacer = false;
        for (JsonElement e : root.getAsJsonArray("skins")) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("id").getAsString().equals("candy_hunt")) {
                art = true;
                assertEquals("YELLOW", o.get("match").getAsString(), "CandyHuntBar's art bar asks for YELLOW");
                for (String k : new String[] {"background", "progress", "meta"}) open(o.get(k).getAsString()).close();
            } else if (o.get("id").getAsString().equals("spacer")) {
                spacer = true;
                assertEquals("BLUE", o.get("match").getAsString(), "CandyHuntBar's spacers ask for BLUE");
                assertTrue(o.get("transparent").getAsBoolean());
            }
        }
        assertTrue(art && spacer);
    }

    @Test
    void metricsFileIsNotShippedToClients() {
        for (String f : CandyHuntPack.FILES) assertFalse(f.contains("hud/"), f);
    }
}
