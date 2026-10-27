package com.spookysurge.hud;

import com.spookysurge.SpookySurge;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Adds the Revenant bar art to the resource pack Polymer builds. Listing the files (instead of adding all of this mod's assets) keeps
 * the download small and, above all, keeps the server's own metrics file out of the pack.
 *
 * <p>Two of these files live in the vanilla namespace and therefore replace vanilla art for the WHOLE server: the YELLOW and BLUE boss-bar
 * sprites. That is the reservation described in docs/bossbar/README.md.
 */
public final class RevenantPack {
    private RevenantPack() {}

    private static final String SPRITES = "assets/minecraft/textures/gui/sprites/boss_bar/";
    private static final String FONT_TEX = "assets/spookysurge/textures/font/revenant/";

    /** Every file added to the pack, relative to the jar root. Tests check that each exists. */
    public static final List<String> FILES = List.of(
            SPRITES + "yellow_background.png",
            SPRITES + "yellow_progress.png",
            SPRITES + "yellow_progress.png.mcmeta",
            SPRITES + "blue_background.png",
            SPRITES + "blue_progress.png",
            "assets/spookysurge/font/revenant.json",
            FONT_TEX + "frame_l.png",
            FONT_TEX + "frame_r.png",
            FONT_TEX + "tier_lit.png",
            FONT_TEX + "letters.png");

    public static void register() {
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder -> {
            try {
                for (String path : FILES) add(builder, path);
            } catch (RuntimeException e) {
                SpookySurge.LOG.error("Could not add the Revenant bar art to the resource pack", e);
            }
        });
    }

    private static void add(ResourcePackBuilder builder, String path) {
        try (InputStream in = RevenantPack.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                SpookySurge.LOG.error("Revenant bar art is missing from the jar: {}", path);
                return;
            }
            builder.addData(path, in.readAllBytes());
        } catch (IOException e) {
            SpookySurge.LOG.error("Could not read Revenant bar art {}: {}", path, e.toString());
        }
    }
}
