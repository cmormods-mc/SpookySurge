package com.spookysurge.item;

import com.spookysurge.SpookySurge;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Adds this mod's item models and textures to the resource pack Polymer builds and serves. Only the
 * item art is copied, so the download stays small.
 */
public final class SurgeItemPack {
    private SurgeItemPack() {}

    /** Every model/texture name under {@code models/item} and {@code textures/item}. */
    static final List<String> NAMES = List.of("candy_corn");

    public static void register() {
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder -> {
            try {
                for (String name : NAMES) {
                    add(builder, "assets/spookysurge/models/item/" + name + ".json");
                    add(builder, "assets/spookysurge/textures/item/" + name + ".png");
                }
            } catch (RuntimeException e) {
                SpookySurge.LOG.error("Could not add the item art to the resource pack", e);
            }
        });
    }

    private static void add(ResourcePackBuilder builder, String path) {
        try (InputStream in = SurgeItemPack.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                SpookySurge.LOG.error("Item art is missing from the jar: {}", path);
                return;
            }
            builder.addData(path, in.readAllBytes());
        } catch (IOException e) {
            SpookySurge.LOG.error("Could not read item art {}: {}", path, e.toString());
        }
    }
}
