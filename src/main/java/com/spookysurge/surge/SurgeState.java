package com.spookysurge.surge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/** Everything that must survive a restart mid-surge, stored with the overworld. */
public final class SurgeState extends SavedData {
    public enum Phase { IDLE, COUNTDOWN, ACTIVE }

    private static final String FILE_ID = "spookysurge";

    public Phase phase = Phase.IDLE;
    /** Wall-clock end of the current phase, epoch millis. */
    public long phaseEndsAt;
    public String surgeId = "";
    public int counter;
    public int maxTier;
    public boolean visitorSpawned;
    public final Map<UUID, Integer> actions = new HashMap<>();
    public final Map<UUID, String> names = new HashMap<>();
    /** Candy Corn owed to players who were not online when it was earned. */
    public final Map<UUID, Integer> pending = new HashMap<>();
    /** Schedule slots already run, so a restart cannot start one twice. */
    public final Set<String> startedSlots = new HashSet<>();
    /** World Boss ids already paid Candy Corn, so a defeat heard twice (a restart) pays once. */
    public final Set<String> paidBosses = new HashSet<>();

    public static SavedData.Factory<SurgeState> factory() {
        return new SavedData.Factory<>(SurgeState::new, SurgeState::load, DataFixTypes.LEVEL);
    }

    public static SurgeState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), FILE_ID);
    }

    static SurgeState load(CompoundTag tag, HolderLookup.Provider registries) {
        SurgeState state = new SurgeState();
        try {
            state.phase = Phase.valueOf(tag.getString("phase"));
        } catch (IllegalArgumentException e) {
            state.phase = Phase.IDLE;
        }
        state.phaseEndsAt = tag.getLong("phase_ends_at");
        state.surgeId = tag.getString("surge_id");
        state.counter = tag.getInt("counter");
        state.maxTier = tag.getInt("max_tier");
        state.visitorSpawned = tag.getBoolean("visitor_spawned");
        ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag row = players.getCompound(i);
            UUID id = row.getUUID("id");
            state.actions.put(id, row.getInt("actions"));
            state.names.put(id, row.getString("name"));
            if (row.getInt("pending") > 0) state.pending.put(id, row.getInt("pending"));
        }
        ListTag slots = tag.getList("slots", Tag.TAG_STRING);
        for (int i = 0; i < slots.size(); i++) state.startedSlots.add(slots.getString(i));
        ListTag bosses = tag.getList("bosses", Tag.TAG_STRING);
        for (int i = 0; i < bosses.size(); i++) state.paidBosses.add(bosses.getString(i));
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("phase", phase.name());
        tag.putLong("phase_ends_at", phaseEndsAt);
        tag.putString("surge_id", surgeId);
        tag.putInt("counter", counter);
        tag.putInt("max_tier", maxTier);
        tag.putBoolean("visitor_spawned", visitorSpawned);
        Set<UUID> ids = new HashSet<>(actions.keySet());
        ids.addAll(pending.keySet());
        ListTag players = new ListTag();
        for (UUID id : ids) {
            CompoundTag row = new CompoundTag();
            row.putUUID("id", id);
            row.putInt("actions", actions.getOrDefault(id, 0));
            row.putInt("pending", pending.getOrDefault(id, 0));
            row.putString("name", names.getOrDefault(id, ""));
            players.add(row);
        }
        tag.put("players", players);
        ListTag slots = new ListTag();
        for (String slot : startedSlots) slots.add(StringTag.valueOf(slot));
        tag.put("slots", slots);
        ListTag bosses = new ListTag();
        for (String boss : paidBosses) bosses.add(StringTag.valueOf(boss));
        tag.put("bosses", bosses);
        return tag;
    }
}
