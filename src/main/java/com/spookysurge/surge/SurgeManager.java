package com.spookysurge.surge;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.spookysurge.SpookySurge;
import com.spookysurge.api.SurgeEndedCallback;
import com.spookysurge.config.SurgeConfig;
import com.spookysurge.config.SurgeConfigStore;
import com.spookysurge.item.SurgeItems;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

/**
 * Runs the surge: the schedule, the phases, the bossbar, the spawning around each player, the shared
 * counter and the rewards. Everything is server-thread and every entry point is wrapped so one failure
 * cannot reach the tick.
 */
public final class SurgeManager {
    private static final Random RANDOM = new Random();

    private static SurgeConfig config = new SurgeConfig();
    private static SurgeState state;
    private static RewardLedger ledger = new RewardLedger(60);
    private static ServerBossEvent bar;
    private static long tick;

    /** Per-surge tallies, for the one-line summary in the log. Not persisted: they only exist to make a log readable. */
    private static int spawnedCount;
    private static int creditedCount;
    private static int expiredCount;

    private record Tracked(ResourceKey<Level> dimension, UUID owner, long expiresAtTick) {}

    private static final Map<UUID, Tracked> tracked = new HashMap<>();
    private static final Map<UUID, Long> nextSpawnAt = new HashMap<>();

    private SurgeManager() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> guard("start", () -> {
            config = SurgeConfigStore.load();
            state = SurgeState.get(server);
            ledger = new RewardLedger(config.perPlayerRewardCap);
            ledger.restore(state.actions);
            checkSpecies();
            SpookySurge.LOG.info("Spooky Surge ready (phase {}, counter {})", state.phase, state.counter);
        }));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> guard("stop", () -> {
            removeAllSurgePokemon(server);
            clearBar();
        }));
        ServerTickEvents.END_SERVER_TICK.register(server -> guard("tick", () -> onTick(server)));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> guard("join", () -> onJoin(handler.getPlayer())));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> guard("entity-load", () -> onEntityLoad(entity)));
    }

    // ---------------------------------------------------------------- accessors for commands

    public static SurgeState state() { return state; }
    public static SurgeConfig config() { return config; }
    public static RewardLedger ledger() { return ledger; }

    /** Logs one warning per configured species Cobblemon does not know, so a typo shows at boot rather than at 20:00. */
    private static void checkSpecies() {
        int bad = 0;
        for (SurgeConfig.SpeciesEntry entry : config.species) {
            if (!SurgeSpawner.speciesExists(entry.id)) {
                SpookySurge.LOG.warn("Config species '{}' is not a known Cobblemon species; it will never spawn", entry.id);
                bad++;
            }
        }
        for (String id : config.visitorSpecies) {
            if (!SurgeSpawner.speciesExists(id)) {
                SpookySurge.LOG.warn("Config visitorSpecies '{}' is not a known Cobblemon species; it will never spawn", id);
                bad++;
            }
        }
        if (bad == 0) SpookySurge.LOG.info("Checked {} configured species: all known", config.species.size() + config.visitorSpecies.size());
    }

    /** Raises experience gains during a running surge, if the operator turned the bonus on. */
    public static int adjustExperience(int experience) {
        if (state == null || state.phase != SurgeState.Phase.ACTIVE) return experience;
        return SurgeMath.xpFor(experience, state.maxTier, config);
    }

    public static void reload(MinecraftServer server) {
        config = SurgeConfigStore.load();
        checkSpecies();
        ledger = new RewardLedger(config.perPlayerRewardCap);
        ledger.restore(state.actions);
    }

    public static int target(MinecraftServer server) {
        return SurgeMath.target(server.getPlayerList().getPlayerCount(), config);
    }

    // ---------------------------------------------------------------- phases

    /** Starts a surge whose action begins at {@code start}. Returns false if one is already running. */
    public static boolean startSurge(MinecraftServer server, Instant start, String slotKey) {
        if (state == null || state.phase != SurgeState.Phase.IDLE) return false;
        long now = System.currentTimeMillis();
        long startMs = start.toEpochMilli();
        state.surgeId = UUID.randomUUID().toString();
        state.counter = 0;
        state.maxTier = 0;
        state.visitorSpawned = false;
        state.actions.clear();
        spawnedCount = 0;
        creditedCount = 0;
        expiredCount = 0;
        ledger = new RewardLedger(config.perPlayerRewardCap);
        if (slotKey != null) state.startedSlots.add(slotKey);
        if (startMs > now) {
            state.phase = SurgeState.Phase.COUNTDOWN;
            state.phaseEndsAt = startMs;
            broadcast(server, Component.literal("A Spooky Surge begins in " + Math.max(1, (startMs - now) / 1000)
                    + " seconds. Ghost Pokemon will rise around you!").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            beginActive(server, startMs);
        }
        state.setDirty();
        return true;
    }

    private static void beginActive(MinecraftServer server, long startMs) {
        state.phase = SurgeState.Phase.ACTIVE;
        state.phaseEndsAt = startMs + config.durationMinutes * 60_000L;
        broadcast(server, Component.literal("The Spooky Surge has begun! Catch or defeat the haunted Pokemon "
                + "around you for Candy Corn.").withStyle(ChatFormatting.GOLD));
        state.setDirty();
    }

    public static void stopSurge(MinecraftServer server, boolean announceSummary) {
        if (state == null || state.phase == SurgeState.Phase.IDLE) return;
        boolean wasActive = state.phase == SurgeState.Phase.ACTIVE;
        if (wasActive) {
            SpookySurge.LOG.info("Surge {} ended: spawned {}, credited {}, expired {}, counter {}, tier {}",
                    state.surgeId, spawnedCount, creditedCount, expiredCount, state.counter, state.maxTier);
            SurgeEndedCallback.Summary summary = new SurgeEndedCallback.Summary(state.surgeId, state.maxTier, state.counter,
                    target(server), ledger.snapshot());
            guard("surge-ended-callback", () -> SurgeEndedCallback.EVENT.invoker().onSurgeEnded(summary));
        }
        state.phase = SurgeState.Phase.IDLE;
        removeAllSurgePokemon(server);
        if (wasActive) {
            for (Map.Entry<UUID, Integer> entry : ledger.snapshot().entrySet()) {
                if (entry.getValue() >= config.participationMin && config.participationBonus > 0) {
                    state.pending.merge(entry.getKey(), config.participationBonus, Integer::sum);
                }
            }
            if (announceSummary) announceSummary(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) claim(player);
        }
        clearBar();
        state.setDirty();
    }

    private static void announceSummary(MinecraftServer server) {
        int target = target(server);
        broadcast(server, Component.literal("The Spooky Surge is over. The table caught " + state.counter + " of "
                + target + " (tier " + state.maxTier + ").").withStyle(ChatFormatting.GOLD));
        List<Map.Entry<UUID, Integer>> top = new ArrayList<>(ledger.snapshot().entrySet());
        top.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < Math.min(5, top.size()); i++) {
            broadcast(server, Component.literal((i + 1) + ". " + nameOf(top.get(i).getKey()) + " - " + top.get(i).getValue())
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    // ---------------------------------------------------------------- tick

    private static void onTick(MinecraftServer server) {
        if (state == null || !config.enabled) return;
        tick++;
        if (tick % 20 != 0) return;
        long now = System.currentTimeMillis();
        switch (state.phase) {
            case IDLE -> {
                Optional<SurgeSchedule.Due> due = SurgeSchedule.due(Instant.now(), config, state.startedSlots);
                if (due.isPresent() && !server.getPlayerList().getPlayers().isEmpty()) {
                    startSurge(server, due.get().start(), due.get().key());
                }
            }
            case COUNTDOWN -> {
                if (now >= state.phaseEndsAt) beginActive(server, state.phaseEndsAt);
            }
            case ACTIVE -> {
                if (now >= state.phaseEndsAt) {
                    stopSurge(server, true);
                    return;
                }
                spawnStep(server);
                expireStep(server);
            }
        }
        refreshBar(server);
    }

    private static void spawnStep(MinecraftServer server) {
        int tier = state.maxTier;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) continue;
            if (nextSpawnAt.getOrDefault(player.getUUID(), 0L) > tick) continue;
            nextSpawnAt.put(player.getUUID(), tick + SurgeMath.spawnInterval(tier, config));
            if (tracked.size() >= config.serverCap) return;
            if (ownedBy(player.getUUID()) >= config.perPlayerCap) continue;
            ServerLevel level = player.serverLevel();
            if (level.dimension() != Level.OVERWORLD) continue;
            Optional<BlockPos> pos = SurgeSpawner.findPosition(level, player, config, RANDOM);
            if (pos.isEmpty()) continue;
            SurgeConfig.SpeciesEntry pick = SurgeMath.pickSpecies(config.species, RANDOM);
            if (pick == null) continue;
            spawnTracked(level, pos.get(), pick.id, pick.rarity, player.getUUID());
        }
    }

    private static void spawnTracked(ServerLevel level, BlockPos pos, String species, String rarity, UUID owner) {
        boolean boosted = state.maxTier >= config.shinyTier;
        long expires = tick + config.lifetimeSeconds * 20L;
        Optional<PokemonEntity> entity = SurgeSpawner.spawn(level, pos, species, rarity, state.surgeId, boosted, config, RANDOM,
                id -> tracked.put(id, new Tracked(level.dimension(), owner, expires)));
        if (entity.isEmpty()) {
            SpookySurge.LOG.warn("Could not spawn surge species '{}' (unknown species id?)", species);
        } else {
            spawnedCount++;
            SpookySurge.LOG.debug("Spawned surge {} ({}) at {} for {}", species, rarity, pos, owner);
        }
    }

    private static int ownedBy(UUID player) {
        int count = 0;
        for (Tracked entry : tracked.values()) if (entry.owner().equals(player)) count++;
        return count;
    }

    private static void expireStep(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Tracked>> it = tracked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Tracked> entry = it.next();
            ServerLevel level = server.getLevel(entry.getValue().dimension());
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            boolean expired = tick >= entry.getValue().expiresAtTick();
            if (entity == null || entity.isRemoved()) {
                if (expired || entity != null) it.remove();
                continue;
            }
            if (expired && entity instanceof PokemonEntity pokemon && !pokemon.isBattling() && pokemon.getPokemon().isWild()) {
                entity.discard();
                expiredCount++;
                it.remove();
            }
        }
    }

    private static void removeAllSurgePokemon(MinecraftServer server) {
        for (Map.Entry<UUID, Tracked> entry : tracked.entrySet()) {
            ServerLevel level = server.getLevel(entry.getValue().dimension());
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            if (entity instanceof PokemonEntity pokemon && pokemon.getPokemon().isWild() && !pokemon.isBattling()) entity.discard();
        }
        tracked.clear();
        nextSpawnAt.clear();
    }

    /** A surge Pokemon that loads when it is not tracked is a leftover from a restart or a past surge. */
    private static void onEntityLoad(Entity entity) {
        if (!(entity instanceof PokemonEntity pokemon) || state == null) return;
        if (!SurgeSpawner.isSurge(pokemon.getPokemon())) return;
        if (tracked.containsKey(entity.getUUID())) return;
        if (!pokemon.getPokemon().isWild()) return; // a caught one belongs to a player
        MinecraftServer server = entity.getServer();
        if (server != null) server.execute(entity::discard);
    }

    // ---------------------------------------------------------------- crediting

    /** Called when a player catches or defeats {@code pokemon}. No-op unless it is a surge Pokemon of the running surge. */
    public static void credit(ServerPlayer player, Pokemon pokemon) {
        if (state == null || state.phase != SurgeState.Phase.ACTIVE) return;
        if (!SurgeSpawner.isSurge(pokemon)) return;
        String surge = pokemon.getPersistentData().getString(SurgeSpawner.TAG_SURGE);
        String rarity = pokemon.getPersistentData().getString(SurgeSpawner.TAG_RARITY);
        pokemon.getPersistentData().remove(SurgeSpawner.TAG_SURGE);
        pokemon.getPersistentData().remove(SurgeSpawner.TAG_RARITY);
        if (!surge.equals(state.surgeId)) return;

        RewardLedger.Result result = ledger.record(player.getUUID(), pokemon.getUuid());
        if (result == RewardLedger.Result.DUPLICATE) return;
        MinecraftServer server = player.getServer();
        state.names.put(player.getUUID(), player.getGameProfile().getName());
        state.actions.clear();
        state.actions.putAll(ledger.snapshot());
        state.counter++;
        int amount = 0;
        if (result == RewardLedger.Result.PAID) {
            amount = SurgeMath.candyFor(rarity, state.maxTier, config);
            give(player, amount);
        }
        creditedCount++;
        SpookySurge.LOG.info("Surge credit: {} took a {} {} (paid {} Candy Corn, counter {})",
                player.getGameProfile().getName(), rarity, pokemon.getSpecies().getName(), amount, state.counter);
        player.displayClientMessage(Component.literal(amount > 0 ? "+" + amount + " Candy Corn" : "Surge counter +1")
                .withStyle(ChatFormatting.GOLD), true);
        if (server != null) checkTier(server);
        state.setDirty();
    }

    private static void checkTier(MinecraftServer server) {
        int tier = SurgeMath.tierFor(state.counter, target(server), config.tierFractions);
        if (tier <= state.maxTier) return;
        state.maxTier = tier;
        broadcast(server, Component.literal("The Spooky Surge reaches tier " + tier + "! " + tierPerk(tier))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (tier >= config.visitorTier && !state.visitorSpawned) spawnVisitor(server);
    }

    private static String tierPerk(int tier) {
        StringBuilder perks = new StringBuilder("More haunted Pokemon are rising.");
        if (tier >= config.shinyTier) perks.append(" Shiny odds are up.");
        if (tier >= config.doubleCandyTier) perks.append(" Candy Corn is doubled.");
        return perks.toString();
    }

    private static void spawnVisitor(MinecraftServer server) {
        UUID best = null;
        int most = -1;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int actions = ledger.actionsBy(player.getUUID());
            if (actions > most && player.serverLevel().dimension() == Level.OVERWORLD) {
                most = actions;
                best = player.getUUID();
            }
        }
        ServerPlayer target = best == null ? null : server.getPlayerList().getPlayer(best);
        if (target == null) return;
        Optional<BlockPos> pos = SurgeSpawner.findPosition(target.serverLevel(), target, config, RANDOM);
        if (pos.isEmpty()) return;
        String species = config.visitorSpecies.get(RANDOM.nextInt(config.visitorSpecies.size()));
        long expires = tick + config.lifetimeSeconds * 20L;
        Optional<PokemonEntity> entity = SurgeSpawner.spawn(target.serverLevel(), pos.get(), species, "rare", state.surgeId, true,
                config, RANDOM, id -> tracked.put(id, new Tracked(target.serverLevel().dimension(), target.getUUID(), expires)));
        if (entity.isPresent()) {
            state.visitorSpawned = true;
            broadcast(server, Component.literal("A Haunted " + capitalise(species) + " has appeared near " + target.getGameProfile().getName()
                    + "! The first to catch it wins.").withStyle(ChatFormatting.GOLD));
        }
    }

    // ---------------------------------------------------------------- Candy Corn delivery

    private static void give(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        int left = amount;
        while (left > 0) {
            int n = Math.min(64, left);
            ItemStack stack = new ItemStack(SurgeItems.candyCorn(), n);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            left -= n;
        }
    }

    /** Delivers Candy Corn owed to a player, if any. Returns the amount. */
    public static int claim(ServerPlayer player) {
        if (state == null) return 0;
        Integer owed = state.pending.remove(player.getUUID());
        if (owed == null || owed <= 0) return 0;
        give(player, owed);
        player.sendSystemMessage(Component.literal("You received " + owed + " Candy Corn from the Spooky Surge.")
                .withStyle(ChatFormatting.GOLD));
        state.setDirty();
        return owed;
    }

    private static void onJoin(ServerPlayer player) {
        if (state == null) return;
        claim(player);
        if (bar != null) bar.addPlayer(player);
    }

    // ---------------------------------------------------------------- bossbar

    private static void refreshBar(MinecraftServer server) {
        if (state.phase == SurgeState.Phase.IDLE) {
            clearBar();
            return;
        }
        if (bar == null) {
            // PURPLE: the World Boss bar uses PINK, and AscensionLib reskins that colour.
            bar = new ServerBossEvent(Component.literal("Spooky Surge"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        }
        long now = System.currentTimeMillis();
        if (state.phase == SurgeState.Phase.COUNTDOWN) {
            long left = Math.max(0, (state.phaseEndsAt - now) / 1000);
            bar.setName(Component.literal("Spooky Surge begins in " + left + "s"));
            bar.setProgress(Math.max(0f, Math.min(1f, left / (float) Math.max(1, config.countdownSeconds))));
        } else {
            int target = target(server);
            long left = Math.max(0, (state.phaseEndsAt - now) / 1000);
            bar.setName(Component.literal(String.format("Spooky Surge  |  Tier %d  |  %d / %d  |  %d:%02d left",
                    state.maxTier, state.counter, target, left / 60, left % 60)));
            bar.setProgress(Math.max(0f, Math.min(1f, state.counter / (float) Math.max(1, target))));
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) bar.addPlayer(player);
    }

    private static void clearBar() {
        if (bar != null) bar.removeAllPlayers();
        bar = null;
    }

    // ---------------------------------------------------------------- helpers

    /** Sets the counter to a tier's threshold. For operators testing. */
    public static void forceTier(MinecraftServer server, int tier) {
        int target = target(server);
        state.counter = Math.max(state.counter, (int) Math.ceil(target * config.tierFractions.get(tier - 1)));
        checkTier(server);
        state.setDirty();
    }

    public static void addToCounter(MinecraftServer server, int amount) {
        state.counter = Math.max(0, state.counter + amount);
        checkTier(server);
        state.setDirty();
    }

    private static String capitalise(String id) {
        return id == null || id.isEmpty() ? "Pokemon" : Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    public static String nameOf(UUID id) {
        String name = state == null ? null : state.names.get(id);
        return name == null || name.isEmpty() ? id.toString().substring(0, 8) : name;
    }

    private static void broadcast(MinecraftServer server, Component message) {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void guard(String what, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            SpookySurge.LOG.error("Spooky Surge: {} failed; the surge continues", what, e);
        }
    }
}
