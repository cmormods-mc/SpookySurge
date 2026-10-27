package com.spookysurge.surge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Starts CobbleRaids' World Boss through its public API ({@code CobbleRaidsWorldBoss.start}), by reflection so this mod has no dependency on
 * CobbleRaids: without it, or with a version that predates the call, the answer is simply "did not start" and the surge falls back.
 */
final class WorldBossBridge {
    private WorldBossBridge() {}

    static final String API_CLASS = "com.cobbleraids.api.worldboss.CobbleRaidsWorldBoss";

    /** @param started the boss is up; @param alreadyActive a World Boss was already running, so there was nothing to start */
    record Result(boolean started, boolean alreadyActive, String reason) {
        static Result ok() { return new Result(true, false, null); }
        static Result no(String reason) { return new Result(false, reason != null && reason.contains("already active"), reason); }
    }

    /** What the surge does with the answer. */
    enum Outcome { BOSS_STARTED, BOSS_ALREADY_RUNNING, FALL_BACK_TO_VISITOR, NOTHING }

    static Outcome decide(Result result, boolean fallbackToVisitor) {
        if (result.started()) return Outcome.BOSS_STARTED;
        if (result.alreadyActive()) return Outcome.BOSS_ALREADY_RUNNING;
        return fallbackToVisitor ? Outcome.FALL_BACK_TO_VISITOR : Outcome.NOTHING;
    }

    /** Server thread only. {@code near} may be null (CobbleRaids then picks a player). */
    static Result start(MinecraftServer server, ServerPlayer near) {
        try {
            Class<?> api = Class.forName(API_CLASS);
            Method start = api.getMethod("start", MinecraftServer.class, ServerPlayer.class);
            Object answer = start.invoke(null, server, near);
            return answer == null ? Result.ok() : Result.no(String.valueOf(answer));
        } catch (ClassNotFoundException e) {
            return Result.no("CobbleRaids is not installed");
        } catch (NoSuchMethodException e) {
            return Result.no("this CobbleRaids is too old: it has no CobbleRaidsWorldBoss.start (needs 0.8.191 or newer)");
        } catch (InvocationTargetException e) {
            return Result.no("CobbleRaids threw " + e.getCause());
        } catch (ReflectiveOperationException | LinkageError e) {
            return Result.no(e.toString());
        }
    }
}
