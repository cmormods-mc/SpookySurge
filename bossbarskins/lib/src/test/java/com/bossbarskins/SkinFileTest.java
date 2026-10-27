package com.bossbarskins;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SkinFileTest {
    private static byte[] png(int w, int h, boolean opaque) throws Exception {
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) im.setRGB(x, y, opaque ? 0xFFFF8800 : (x == 0 ? 0 : 0xFFFF8800));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(im, "png", out);
        return out.toByteArray();
    }

    @Test
    void parsesOpaqueAndTransparentSkins() throws Exception {
        String json = "{\"skins\":[{\"id\":\"bar\",\"match\":\"yellow\",\"background\":\"bg.png\",\"progress\":\"pr.png\"},{\"id\":\"spacer\",\"match\":\"BLUE\",\"transparent\":true}]}";
        List<String> problems = new ArrayList<>();
        List<Skin> skins = SkinFile.parse("m", new StringReader(json), p -> {
            try { return p.equals("bg.png") ? png(182, 5, true) : png(182, 80, true); } catch (Exception e) { throw new RuntimeException(e); }
        }, problems);
        assertEquals(2, skins.size(), problems.toString());
        assertTrue(skins.get(0).opaque());
        assertTrue(skins.get(1).transparent());
        assertEquals("m:bar", skins.get(0).key());
    }

    @Test
    void aTranslucentSpriteIsNotOpaque() throws Exception {
        List<String> problems = new ArrayList<>();
        List<Skin> skins = SkinFile.parse("m", new StringReader("{\"skins\":[{\"id\":\"bar\",\"match\":\"YELLOW\",\"background\":\"bg.png\",\"progress\":\"pr.png\"}]}"),
                p -> { try { return png(182, 5, p.equals("bg.png")); } catch (Exception e) { throw new RuntimeException(e); } }, problems);
        assertFalse(skins.get(0).opaque());
    }

    @Test
    void badSkinsAreSkippedWithAReason() throws Exception {
        List<String> problems = new ArrayList<>();
        String json = "{\"skins\":[{\"id\":\"Bad Id\",\"match\":\"YELLOW\",\"background\":\"a\",\"progress\":\"b\"},"
                + "{\"id\":\"wide\",\"match\":\"YELLOW\",\"background\":\"w\",\"progress\":\"w\"},"
                + "{\"id\":\"gone\",\"match\":\"YELLOW\",\"background\":\"nope\",\"progress\":\"nope\"},"
                + "{\"id\":\"color\",\"match\":\"MAUVE\",\"background\":\"w\",\"progress\":\"w\"}]}";
        List<Skin> skins = SkinFile.parse("m", new StringReader(json), p -> {
            try { return p.equals("nope") ? null : png(100, 5, true); } catch (Exception e) { throw new RuntimeException(e); }
        }, problems);
        assertTrue(skins.isEmpty());
        assertEquals(4, problems.size(), problems.toString());
    }

    @Test
    void garbageJsonIsReportedNotThrown() {
        List<String> problems = new ArrayList<>();
        assertTrue(SkinFile.parse("m", new StringReader("not json"), p -> null, problems).isEmpty());
        assertEquals(1, problems.size());
    }

    @Test
    void progressMayBeAStackOfFramesButBackgroundMayNot() throws Exception {
        assertTrue(SkinFile.checkSprite(png(182, 80, true), true, "pr"));
        assertThrows(java.io.IOException.class, () -> SkinFile.checkSprite(png(182, 80, true), false, "bg"));
        assertThrows(java.io.IOException.class, () -> SkinFile.checkSprite(png(182, 7, true), true, "pr"));
    }
}
