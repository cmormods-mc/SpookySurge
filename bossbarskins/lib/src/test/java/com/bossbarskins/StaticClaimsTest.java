package com.bossbarskins;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StaticClaimsTest {
    @Test
    void findsSpritesNestedUnderAnyFolderAndSkipsCode(@TempDir Path root) throws Exception {
        Path dir = root.resolve("ascensionlib-pack/assets/minecraft/textures/gui/sprites/boss_bar");
        Files.createDirectories(dir);
        for (String n : new String[] {"pink_background.png", "pink_progress.png", "notched_20_background.png", "notched_20_progress.png", "readme.txt"})
            Files.write(dir.resolve(n), new byte[] {1});
        Path code = root.resolve("com/example/assets/minecraft/textures/gui/sprites/boss_bar");
        Files.createDirectories(code);
        Files.write(code.resolve("green_progress.png"), new byte[] {1});
        var found = StaticClaims.scan(root);
        assertEquals(java.util.Set.of("pink", "notched_20"), found.keySet());
    }

    @Test
    void anUnreadableRootClaimsNothing() {
        assertTrue(StaticClaims.scan(Path.of("/definitely/not/here")).isEmpty());
    }
}
