package com.bossbarskins;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.world.BossEvent;

/**
 * Works out which mod created a boss bar: the class that called the {@code ServerBossEvent} constructor. A bar created by vanilla code
 * has no owner, so vanilla bosses are never touched even when a mod's code triggered their creation.
 */
public final class OwnerResolver {
    private OwnerResolver() {}

    private static final String NONE = "";
    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();

    /** The mod id of whoever is constructing a boss event right now, or null for vanilla or unknown callers. */
    public static String currentOwner() {
        Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(frames -> frames
                .map(StackWalker.StackFrame::getDeclaringClass)
                .filter(c -> !BossEvent.class.isAssignableFrom(c) && !isInfrastructure(c.getName()))
                .findFirst()).orElse(null);
        if (caller == null) return null;
        String owner = ownerOfClass(caller.getName());
        return owner.isEmpty() ? null : owner;
    }

    private static boolean isInfrastructure(String name) {
        return name.startsWith("com.bossbarskins.") || name.startsWith("java.") || name.startsWith("jdk.") || name.startsWith("sun.")
                || name.startsWith("org.spongepowered.") || name.startsWith("kotlin.");
    }

    static String ownerOfClass(String className) {
        return CACHE.computeIfAbsent(className, OwnerResolver::lookup);
    }

    private static String lookup(String className) {
        if (className.startsWith("net.minecraft.") || className.startsWith("com.mojang.")) return NONE;
        int lambda = className.indexOf("$$Lambda");
        String base = lambda > 0 ? className.substring(0, lambda) : className;
        String resource = base.replace('.', '/') + ".class";
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            String id = mod.getMetadata().getId();
            if (id.equals("minecraft") || id.equals("java") || id.equals("fabricloader")) continue;
            Optional<java.nio.file.Path> found = mod.findPath(resource);
            if (found.isPresent()) return id;
        }
        return NONE;
    }
}
