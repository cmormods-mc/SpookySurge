package com.spookysurge.hud;

import com.spookysurge.SpookySurge;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Adds the CandyHunt bar art to the resource pack Polymer builds. Listing the files (instead of adding all of this mod's assets) keeps
 * the download small and, above all, keeps the server's own metrics file out of the pack.
 *
 * <p>Two of these files live in the vanilla namespace and therefore replace vanilla art for the WHOLE server: the YELLOW and BLUE boss-bar
 * sprites. That is the reservation described in docs/bossbar/README.md.
 */
public final class CandyHuntPack {
    private CandyHuntPack() {}

    private static final String SPRITES = "assets/minecraft/textures/gui/sprites/boss_bar/";
    private static final String SKIN = "bossbarskins/candyhunt/";
    private static final String FONT_TEX = "assets/spookysurge/textures/font/candyhunt/";

    /** The art bar's sprites and the declaration Boss Bar Skins reads. Tests check that each exists. */
    public static final List<String> SKIN_FILES = List.of(SKIN + "background.png", SKIN + "progress.png", SKIN + "progress.png.mcmeta", SKIN + "transparent.png",
            "bossbarskins.json");

    /** Files added to the pack in every case: the font. */
    public static final List<String> FONT_FILES = List.of(
            "assets/spookysurge/font/candyhunt.json",
            FONT_TEX + "frame_l.png",
            FONT_TEX + "frame_r.png",
            FONT_TEX + "tier_lit.png",
            FONT_TEX + "letters.png",
            FONT_TEX + "timer.png");

    /** Every file the jar must contain for this feature. */
    public static final List<String> FILES = java.util.stream.Stream.concat(SKIN_FILES.stream(), FONT_FILES.stream()).toList();

    /** Without Boss Bar Skins this mod puts its sprites on YELLOW and BLUE itself. With it, the library places them (and resolves clashes with other mods). */
    private static final java.util.Map<String, String> STANDALONE = java.util.Map.of(
            SKIN + "background.png", SPRITES + "yellow_background.png",
            SKIN + "progress.png", SPRITES + "yellow_progress.png",
            SKIN + "progress.png.mcmeta", SPRITES + "yellow_progress.png.mcmeta",
            SKIN + "transparent.png", SPRITES + "blue_background.png");

    public static void register() {
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder -> {
            try {
                for (String path : FONT_FILES) add(builder, path);
                if (FabricLoader.getInstance().isModLoaded("bossbarskins")) {
                    SpookySurge.LOG.info("Boss Bar Skins is installed: it will place the art bar's sprites");
                } else {
                    for (java.util.Map.Entry<String, String> e : STANDALONE.entrySet()) add(builder, e.getKey(), e.getValue());
                    add(builder, SKIN + "transparent.png", SPRITES + "blue_progress.png");
                }
            } catch (RuntimeException e) {
                SpookySurge.LOG.error("Could not add the CandyHunt bar art to the resource pack", e);
            }
        });
    }

    private static void add(ResourcePackBuilder builder, String path) {
        add(builder, path, path);
    }

    private static void add(ResourcePackBuilder builder, String path, String packPath) {
        try (InputStream in = CandyHuntPack.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                SpookySurge.LOG.error("CandyHunt bar art is missing from the jar: {}", path);
                return;
            }
            builder.addData(packPath, in.readAllBytes());
        } catch (IOException e) {
            SpookySurge.LOG.error("Could not read CandyHunt bar art {}: {}", path, e.toString());
        }
    }
}
