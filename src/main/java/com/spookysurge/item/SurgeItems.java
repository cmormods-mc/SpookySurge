package com.spookysurge.item;

import com.spookysurge.SpookySurge;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** The items Spooky Surge registers. Candy Corn is the event currency a vendor can sell or accept. */
public final class SurgeItems {
    private SurgeItems() {}

    public static final ResourceLocation CANDY_CORN_ID =
            ResourceLocation.fromNamespaceAndPath(SpookySurge.MOD_ID, "candy_corn");

    private static Item candyCorn;

    public static Item candyCorn() {
        return candyCorn;
    }

    /** Idempotent: a second registration of the same id would throw and take the mod down at load. */
    public static void register() {
        if (candyCorn != null) return;
        candyCorn = Registry.register(BuiltInRegistries.ITEM, CANDY_CORN_ID,
                new SurgePolymerItem(new Item.Properties().stacksTo(64), Items.SUGAR, "candy_corn", "Candy Corn",
                        List.of("A Halloween treat from the Spooky Surge.")));
    }
}
