package com.bossbarskins;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds boss bar sprites that a mod ships directly under {@code assets/minecraft/textures/gui/sprites/boss_bar/} somewhere in its jar
 * (AscensionLib's World Boss bar does: {@code pink_*} and {@code notched_20_*}, which Polymer then copies into the server pack). Those slots
 * are already skinned without this library, so nobody else may be handed them: a second writer would silently replace the first one's art.
 */
public final class StaticClaims {
    private StaticClaims() {}

    private static final Pattern SPRITE = Pattern.compile(".*assets/minecraft/textures/gui/sprites/boss_bar/([a-z0-9_]+?)_(background|progress)\\.png");
    private static final Set<String> SKIP_TOP = Set.of("com", "net", "org", "io", "kotlin", "kotlinx", "javax", "META-INF", "it", "me", "dev");

    /** Slot identity (for example "pink" or "notched_20") -> true, for every sprite pair found under {@code root}. */
    public static Map<String, Boolean> scan(Path root) {
        Map<String, Boolean> found = new LinkedHashMap<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.getNameCount() - root.getNameCount() == 1 && SKIP_TOP.contains(dir.getFileName().toString().replace("/", ""))) return FileVisitResult.SKIP_SUBTREE;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    Matcher m = SPRITE.matcher(file.toString().replace('\\', '/'));
                    if (m.matches()) found.put(m.group(1), true);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException | RuntimeException e) {
            // an unreadable jar simply claims nothing
        }
        return found;
    }
}
