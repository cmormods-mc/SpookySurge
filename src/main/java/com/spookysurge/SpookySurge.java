package com.spookysurge;

import com.spookysurge.item.SurgeItemPack;
import com.spookysurge.item.SurgeItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entrypoint. Server-side only: nothing here needs a client class or a custom packet. */
public final class SpookySurge implements ModInitializer {
    public static final String MOD_ID = "spookysurge";
    public static final Logger LOG = LoggerFactory.getLogger("SpookySurge");

    @Override
    public void onInitialize() {
        SurgeItems.register();
        SurgeItemPack.register();
        LOG.info("Spooky Surge loaded");
    }
}
