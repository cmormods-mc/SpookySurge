package com.spookysurge.surge;

import com.spookysurge.SpookySurge;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Pays Candy Corn to everyone who fought CobbleRaids' World Boss when it is defeated. CobbleRaids is reached by reflection (no dependency); without
 * it, or with a different API version, nothing happens. The amount is {@code worldBossCandy} in the config.
 *
 * <p>Candy goes through the same owed-Candy list as surge rewards, so an offline fighter gets it on joining. The boss id is saved once paid: the
 * listener can hear one defeat twice after a restart.
 */
final class WorldBossCandy {
    private static final String API = "com.cobbleraids.api.worldboss.CobbleRaidsWorldBoss";
    private static final String LISTENER = "com.cobbleraids.api.worldboss.WorldBossListener";
    private static final int SUPPORTED_API_VERSION = 1;

    private static volatile Object registered;

    private WorldBossCandy() {}

    /** Candy Corn owed per fighter: everyone who dealt any damage gets the flat amount. Pure, for tests. */
    static Map<UUID, Integer> payouts(Map<UUID, Float> damage, int amount) {
        Map<UUID, Integer> owed = new HashMap<>();
        if (amount <= 0) return owed;
        for (Map.Entry<UUID, Float> entry : damage.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0f) owed.put(entry.getKey(), amount);
        }
        return owed;
    }

    /** Registers once per JVM; a second call replaces the first listener so a boss never pays twice. */
    static synchronized void register(MinecraftServer server) {
        try {
            Class<?> api = Class.forName(API);
            Class<?> listenerType = Class.forName(LISTENER);
            if (api.getField("API_VERSION").getInt(null) != SUPPORTED_API_VERSION) {
                SpookySurge.LOG.warn("CobbleRaids' World Boss API is not version {}: World Boss Candy Corn is off", SUPPORTED_API_VERSION);
                return;
            }
            Method add = api.getMethod("addListener", listenerType);
            Method remove = api.getMethod("removeListener", listenerType);
            if (registered != null) remove.invoke(null, registered);
            Object proxy = Proxy.newProxyInstance(listenerType.getClassLoader(), new Class<?>[] {listenerType}, (self, method, args) -> {
                switch (method.getName()) {
                    case "onDefeated" -> {
                        @SuppressWarnings("unchecked") Map<UUID, Float> damage = (Map<UUID, Float>) args[2];
                        try {
                            paid(server, (UUID) args[0], damage);
                        } catch (RuntimeException e) {
                            SpookySurge.LOG.error("World Boss Candy Corn failed for boss {}", args[0], e);
                        }
                        return null;
                    }
                    case "toString" -> { return "Spooky Surge World Boss listener"; }
                    case "hashCode" -> { return System.identityHashCode(self); }
                    case "equals" -> { return self == args[0]; }
                    default -> { return null; }
                }
            });
            add.invoke(null, proxy);
            registered = proxy;
            SpookySurge.LOG.info("Listening for the CobbleRaids World Boss: a defeat pays {} Candy Corn to each fighter",
                    SurgeManager.config().worldBossCandy);
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException absent) {
            SpookySurge.LOG.info("CobbleRaids has no World Boss API here; World Boss Candy Corn is off");
        } catch (ReflectiveOperationException | LinkageError unexpected) {
            SpookySurge.LOG.warn("CobbleRaids' World Boss API could not be reached ({}); World Boss Candy Corn is off", unexpected.toString());
        }
    }

    /** Server thread. Owes the Candy Corn, then hands it to whoever is online. */
    private static void paid(MinecraftServer server, UUID bossId, Map<UUID, Float> damage) {
        SurgeState state = SurgeManager.state();
        if (state == null) return;
        if (!state.paidBosses.add(bossId.toString())) return;
        Map<UUID, Integer> owed = payouts(damage, SurgeManager.config().worldBossCandy);
        owed.forEach((player, amount) -> state.pending.merge(player, amount, Integer::sum));
        state.setDirty();
        for (UUID id : owed.keySet()) {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online != null) SurgeManager.claim(online);
        }
        SpookySurge.LOG.info("World Boss {} defeated: {} fighter(s) owed {} Candy Corn each", bossId, owed.size(), SurgeManager.config().worldBossCandy);
    }
}
