package com.bbstest;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

/**
 * Deliberately naive: it uses the plain vanilla API and knows nothing about Boss Bar Skins. {@code /bbstest <percent>} shows the
 * executor a YELLOW bar (the colour Spooky Surge also reskins) and an unskinned PURPLE bar, until {@code /bbstest off}.
 */
public final class BbsTest implements ModInitializer {
    private static ServerBossEvent yellow, purple;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> dispatcher.register(Commands.literal("bbstest").requires(s -> s.hasPermission(2))
                .then(Commands.literal("off").executes(ctx -> {
                    clear();
                    return 1;
                }))
                .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    clear();
                    float p = IntegerArgumentType.getInteger(ctx, "percent") / 100f;
                    yellow = new ServerBossEvent(Component.literal("Other mod: asks for YELLOW"), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
                    purple = new ServerBossEvent(Component.literal("Other mod: asks for PURPLE (no skin)"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
                    yellow.setProgress(p);
                    purple.setProgress(p);
                    yellow.addPlayer(player);
                    purple.addPlayer(player);
                    return 1;
                }))));
    }

    private static void clear() {
        if (yellow != null) yellow.removeAllPlayers();
        if (purple != null) purple.removeAllPlayers();
        yellow = purple = null;
    }
}
