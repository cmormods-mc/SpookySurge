package com.spookysurge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spookysurge.SpookySurge;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/** Reads and writes {@code config/spookysurge/server.json}. A broken file never stops the server: it is logged and defaults are used. */
public final class SurgeConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SurgeConfigStore() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("spookysurge").resolve("server.json");
    }

    public static SurgeConfig load() {
        Path file = path();
        SurgeConfig config = null;
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                config = GSON.fromJson(reader, SurgeConfig.class);
            } catch (IOException | RuntimeException e) {
                SpookySurge.LOG.error("Could not read {}; using the defaults: {}", file, e.toString());
            }
        }
        boolean fresh = config == null;
        if (fresh) config = new SurgeConfig();
        List<String> problems = new ArrayList<>();
        config.validated(problems);
        for (String problem : problems) SpookySurge.LOG.warn("Config repaired: {}", problem);
        if (fresh || !problems.isEmpty()) save(config);
        return config;
    }

    private static void save(SurgeConfig config) {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            SpookySurge.LOG.error("Could not write {}: {}", file, e.toString());
        }
    }
}
