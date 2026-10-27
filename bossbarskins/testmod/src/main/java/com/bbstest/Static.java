package com.bbstest;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.loader.api.FabricLoader;

/** Mirrors AscensionLib's PolymerPack: copy a folder of the jar into the Polymer pack, overwriting. */
final class Static {
    private Static() {}

    static void register() {
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(builder ->
                FabricLoader.getInstance().getModContainer("bbstest").flatMap(m -> m.findPath("fakeascension-pack")).ifPresent(root -> builder.copyFromPath(root, "", true)));
    }
}
