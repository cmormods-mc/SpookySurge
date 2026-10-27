package com.spookysurge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.spookysurge.surge.SurgeManager;
import com.spookysurge.surge.SurgeState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /surge} for everyone; the subcommands that change anything need operator level 2, like the other mods. */
public final class SurgeCommands {
    private SurgeCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> build(dispatcher));
    }

    private static void build(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("surge")
                .executes(SurgeCommands::status)
                .then(Commands.literal("status").executes(SurgeCommands::status))
                .then(Commands.literal("top").executes(SurgeCommands::top))
                .then(Commands.literal("claim").executes(SurgeCommands::claim))
                .then(Commands.literal("start").requires(s -> s.hasPermission(2))
                        .executes(ctx -> start(ctx, 0))
                        .then(Commands.argument("delaySeconds", IntegerArgumentType.integer(0, 600))
                                .executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "delaySeconds")))))
                .then(Commands.literal("stop").requires(s -> s.hasPermission(2)).executes(SurgeCommands::stop))
                .then(Commands.literal("reload").requires(s -> s.hasPermission(2)).executes(SurgeCommands::reload))
                .then(Commands.literal("add").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-100_000, 100_000))
                                .executes(ctx -> add(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))
                .then(Commands.literal("tier").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("tier", IntegerArgumentType.integer(1, 4))
                                .executes(ctx -> tier(ctx, IntegerArgumentType.getInteger(ctx, "tier"))))));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        SurgeState state = SurgeManager.state();
        if (state == null) return fail(ctx, "Spooky Surge is not ready yet.");
        CommandSourceStack source = ctx.getSource();
        switch (state.phase) {
            case IDLE -> reply(source, "No surge is running. Surges start on a schedule in the event window.");
            case COUNTDOWN -> reply(source, "A surge begins in " + Math.max(0, (state.phaseEndsAt - System.currentTimeMillis()) / 1000) + "s.");
            case ACTIVE -> {
                long left = Math.max(0, (state.phaseEndsAt - System.currentTimeMillis()) / 1000);
                reply(source, "Surge running: tier " + state.maxTier + ", " + state.counter + " / "
                        + SurgeManager.target(source.getServer()) + ", " + left / 60 + ":" + String.format("%02d", left % 60) + " left.");
                reply(source, SurgeManager.progressLine());
                if (source.hasPermission(2)) reply(source, "Surge id: " + state.surgeId);
                if (source.getEntity() instanceof ServerPlayer player) {
                    reply(source, "Your catches and defeats: " + SurgeManager.ledger().actionsBy(player.getUUID()));
                }
            }
        }
        return 1;
    }

    private static int top(CommandContext<CommandSourceStack> ctx) {
        if (SurgeManager.state() == null) return fail(ctx, "Spooky Surge is not ready yet.");
        List<Map.Entry<UUID, Integer>> rows = new ArrayList<>(SurgeManager.ledger().snapshot().entrySet());
        rows.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        if (rows.isEmpty()) return fail(ctx, "Nobody has caught a surge Pokemon yet.");
        for (int i = 0; i < Math.min(10, rows.size()); i++) {
            reply(ctx.getSource(), (i + 1) + ". " + SurgeManager.nameOf(rows.get(i).getKey()) + " - " + rows.get(i).getValue());
        }
        return 1;
    }

    private static int claim(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) return fail(ctx, "Only a player can claim.");
        int amount = SurgeManager.claim(player);
        if (amount == 0) reply(ctx.getSource(), "You have no Candy Corn waiting.");
        return 1;
    }

    private static int start(CommandContext<CommandSourceStack> ctx, int delaySeconds) {
        boolean ok = SurgeManager.startSurge(ctx.getSource().getServer(), Instant.now().plusSeconds(delaySeconds), null);
        return ok ? ok(ctx, "Surge started.") : fail(ctx, "A surge is already running.");
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) {
        SurgeManager.stopSurge(ctx.getSource().getServer(), true);
        return ok(ctx, "Surge stopped.");
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        SurgeManager.reload(ctx.getSource().getServer());
        return ok(ctx, "Config reloaded.");
    }

    private static int add(CommandContext<CommandSourceStack> ctx, int amount) {
        if (SurgeManager.state() == null || SurgeManager.state().phase != SurgeState.Phase.ACTIVE) return fail(ctx, "No surge is active.");
        SurgeManager.addToCounter(ctx.getSource().getServer(), amount);
        return ok(ctx, "Counter is now " + SurgeManager.state().counter + ".");
    }

    private static int tier(CommandContext<CommandSourceStack> ctx, int tier) {
        if (SurgeManager.state() == null || SurgeManager.state().phase != SurgeState.Phase.ACTIVE) return fail(ctx, "No surge is active.");
        SurgeManager.forceTier(ctx.getSource().getServer(), tier);
        return ok(ctx, "Counter raised to tier " + tier + ".");
    }

    private static void reply(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, String text) {
        reply(ctx.getSource(), text);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendFailure(Component.literal(text));
        return 0;
    }
}
