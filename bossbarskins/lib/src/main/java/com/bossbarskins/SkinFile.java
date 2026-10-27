package com.bossbarskins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.imageio.ImageIO;

/**
 * Reads a mod's {@code bossbarskins.json} (at the root of its jar):
 * <pre>
 * { "skins": [
 *   { "id": "candy_hunt", "match": "YELLOW",            // what the mod's code asks for: COLOUR or COLOUR/OVERLAY
 *     "background": "bossbarskins/candyhunt/background.png",   // paths inside the mod's jar, 182 x 5 (progress may be 182 x 5n + .mcmeta)
 *     "progress": "bossbarskins/candyhunt/progress.png",
 *     "meta": "bossbarskins/candyhunt/progress.png.mcmeta" },    // optional animation
 *   { "id": "spacer", "match": "BLUE", "transparent": true } ] }
 * </pre>
 */
public final class SkinFile {
    private SkinFile() {}

    /** Parses and validates; problems are appended and the offending skin is skipped. {@code reader} loads a jar path's bytes, or returns null when absent. */
    public static List<Skin> parse(String owner, Reader json, Function<String, byte[]> reader, List<String> problems) {
        List<Skin> out = new ArrayList<>();
        JsonArray skins;
        try {
            skins = JsonParser.parseReader(json).getAsJsonObject().getAsJsonArray("skins");
        } catch (RuntimeException e) {
            problems.add(owner + ": bossbarskins.json is not valid (" + e.getMessage() + ")");
            return out;
        }
        if (skins == null) return out;
        for (JsonElement e : skins) {
            try {
                JsonObject o = e.getAsJsonObject();
                String id = o.get("id").getAsString();
                if (!id.matches("[a-z0-9_]+")) throw new IllegalArgumentException("id must be lowercase letters, digits and underscores");
                Slot requested = Slot.parse(o.get("match").getAsString());
                if (o.has("transparent") && o.get("transparent").getAsBoolean()) {
                    out.add(new Skin(owner, id, requested, true, false, null, null, null));
                    continue;
                }
                String bg = o.get("background").getAsString(), pr = o.get("progress").getAsString();
                String meta = o.has("meta") ? o.get("meta").getAsString() : null;
                byte[] bgBytes = reader.apply(bg), prBytes = reader.apply(pr);
                if (bgBytes == null) throw new IllegalArgumentException("missing file " + bg);
                if (prBytes == null) throw new IllegalArgumentException("missing file " + pr);
                if (meta != null && reader.apply(meta) == null) throw new IllegalArgumentException("missing file " + meta);
                boolean opaqueBg = checkSprite(bgBytes, false, bg), opaquePr = checkSprite(prBytes, true, pr);
                out.add(new Skin(owner, id, requested, false, opaqueBg && opaquePr, bg, pr, meta));
            } catch (RuntimeException | IOException ex) {
                problems.add(owner + ": skipped a skin (" + ex.getMessage() + ")");
            }
        }
        return out;
    }

    /** A sprite must be a PNG 182 wide and 5 tall (the progress sprite may be a stack of frames). Returns whether every pixel is opaque. */
    static boolean checkSprite(byte[] png, boolean animatedAllowed, String name) throws IOException {
        BufferedImage im = ImageIO.read(new ByteArrayInputStream(png));
        if (im == null) throw new IOException(name + " is not a PNG");
        if (im.getWidth() != 182) throw new IOException(name + " must be 182 px wide, it is " + im.getWidth());
        boolean heightOk = animatedAllowed ? im.getHeight() % 5 == 0 && im.getHeight() > 0 : im.getHeight() == 5;
        if (!heightOk) throw new IOException(name + " has the wrong height " + im.getHeight() + (animatedAllowed ? " (a multiple of 5)" : " (5)"));
        for (int y = 0; y < im.getHeight(); y++) for (int x = 0; x < 182; x++) if ((im.getRGB(x, y) >>> 24) != 255) return false;
        return true;
    }
}
