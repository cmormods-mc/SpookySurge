package com.bossbarskins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.imageio.ImageIO;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Boss Bar Skins. Mods keep using the vanilla boss bar API; each declares its reskins in {@code bossbarskins.json}; this mod gives every
 * skin its own sprite slot, builds the sprites into the Polymer pack under the right names and rewrites the colour and overlay of that
 * mod's bars on the wire. A mod alone on a colour is not remapped at all. See docs/README.md.
 */
public final class BossBarSkins implements ModInitializer {
    public static final String MOD_ID = "bossbarskins";
    public static final Logger LOG = LoggerFactory.getLogger("BossBarSkins");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Optional operator settings: {@code config/bossbarskins.json}. */
    public static final class Config {
        /** Slot identities nobody may be relocated into (vanilla's own bosses use pink, purple and red). */
        public List<String> reserved = new ArrayList<>(SlotAllocator.defaultReserved());
        /** "modid:skinid" -> "YELLOW" or "YELLOW/NOTCHED_6": force a skin into a slot. */
        public Map<String, String> pins = new LinkedHashMap<>();
    }

    private static final Map<String, Skin> SKINS = new LinkedHashMap<>();
    private static SlotAllocator.Result result;
    private static final Map<String, byte[]> SPRITE_BYTES = new HashMap<>();

    @Override
    public void onInitialize() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        Config config = readJson(dir.resolve("config.json"), Config.class, new Config());
        Map<String, String> savedRaw = readJson(dir.resolve("assignments.json"), JsonObjectMap.class, new JsonObjectMap()).assignments;
        List<String> problems = new ArrayList<>();
        Map<String, ModContainer> owners = new HashMap<>();

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            Optional<Path> file = mod.findPath("bossbarskins.json");
            if (file.isEmpty()) continue;
            String owner = mod.getMetadata().getId();
            try (var reader = new InputStreamReader(Files.newInputStream(file.get()), StandardCharsets.UTF_8)) {
                for (Skin s : SkinFile.parse(owner, reader, path -> readFromMod(mod, path), problems)) {
                    SKINS.put(s.key(), s);
                    owners.put(owner, mod);
                }
            } catch (IOException e) {
                problems.add(owner + ": cannot read bossbarskins.json (" + e.getMessage() + ")");
            }
        }
        Map<String, Slot> pins = parseSlots(config.pins, problems), previous = parseSlots(savedRaw, problems);
        result = SlotAllocator.allocate(new ArrayList<>(SKINS.values()), previous, pins, new HashSet<>(config.reserved), SlotAllocator.defaultPool());
        problems.addAll(result.problems());
        for (Skin s : SKINS.values()) {
            Slot slot = result.assigned().get(s.key());
            if (slot != null && !s.transparent()) {
                SPRITE_BYTES.put(s.key() + "|bg", readFromMod(owners.get(s.owner()), s.background()));
                SPRITE_BYTES.put(s.key() + "|pr", readFromMod(owners.get(s.owner()), s.progress()));
                if (s.meta() != null) SPRITE_BYTES.put(s.key() + "|meta", readFromMod(owners.get(s.owner()), s.meta()));
            }
        }
        SkinRuntime.install(result.assigned(), SKINS);
        saveAssignments(dir);
        for (String p : problems) LOG.warn("{}", p);
        LOG.info("Boss Bar Skins: {} skin(s) from {} mod(s), {} placed", SKINS.size(), owners.size(), result.assigned().size());

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(this::writeSprites);
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> register(dispatcher));
    }

    // ------------------------------------------------------------------------------------------------ pack

    private void writeSprites(ResourcePackBuilder builder) {
        try {
            for (Skin s : SKINS.values()) {
                Slot slot = result.assigned().get(s.key());
                if (slot == null) continue;
                String base = "assets/minecraft/textures/gui/sprites/boss_bar/" + slot.identity();
                if (s.transparent()) {
                    byte[] clear = transparentPng();
                    builder.addData(base + "_background.png", clear);
                    builder.addData(base + "_progress.png", clear);
                    continue;
                }
                builder.addData(base + "_background.png", SPRITE_BYTES.get(s.key() + "|bg"));
                builder.addData(base + "_progress.png", SPRITE_BYTES.get(s.key() + "|pr"));
                byte[] meta = SPRITE_BYTES.get(s.key() + "|meta");
                if (meta != null) builder.addData(base + "_progress.png.mcmeta", meta);
            }
        } catch (RuntimeException | IOException e) {
            LOG.error("Boss Bar Skins: could not add the sprites to the resource pack", e);
        }
    }

    private static byte[] transparentPng() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(182, 5, BufferedImage.TYPE_INT_ARGB), "png", out);
        return out.toByteArray();
    }

    // ------------------------------------------------------------------------------------------------ commands

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bossbarskins").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> {
                    if (SKINS.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("No mod has declared a boss bar skin."), false);
                        return 0;
                    }
                    for (Skin s : SKINS.values()) {
                        Slot slot = result.assigned().get(s.key());
                        String line = s.key() + (s.transparent() ? " (transparent)" : "") + ": asks for " + s.requested() + " -> "
                                + (slot == null ? "NO SLOT (vanilla look)" : slot + (slot.equals(s.requested()) ? "" : "  (relocated)"));
                        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                    }
                    return SKINS.size();
                })));
    }

    // ------------------------------------------------------------------------------------------------ files

    private static byte[] readFromMod(ModContainer mod, String path) {
        if (mod == null) return null;
        Optional<Path> p = mod.findPath(path);
        if (p.isEmpty()) return null;
        try {
            return Files.readAllBytes(p.get());
        } catch (IOException e) {
            return null;
        }
    }

    /** Wrapper so Gson can read {@code {"assignments": {"mod:skin": "YELLOW"}}}. */
    static final class JsonObjectMap {
        Map<String, String> assignments = new LinkedHashMap<>();
    }

    private static Map<String, Slot> parseSlots(Map<String, String> raw, List<String> problems) {
        Map<String, Slot> out = new LinkedHashMap<>();
        if (raw == null) return out;
        raw.forEach((key, text) -> {
            try {
                out.put(key, Slot.parse(text));
            } catch (RuntimeException e) {
                problems.add("ignored bad slot '" + text + "' for " + key);
            }
        });
        return out;
    }

    private static void saveAssignments(Path dir) {
        try {
            Files.createDirectories(dir);
            JsonObjectMap map = new JsonObjectMap();
            result.assigned().forEach((key, slot) -> map.assignments.put(key, slot.toString()));
            Files.writeString(dir.resolve("assignments.json"), GSON.toJson(map));
        } catch (IOException e) {
            LOG.warn("Could not save assignments: {}", e.toString());
        }
    }

    private static <T> T readJson(Path file, Class<T> type, T fallback) {
        try {
            if (!Files.exists(file)) {
                if (type == Config.class) {
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, GSON.toJson(fallback));
                }
                return fallback;
            }
            T read = GSON.fromJson(Files.readString(file), type);
            return read == null ? fallback : read;
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not read {} ({}); using defaults", file, e.toString());
            return fallback;
        }
    }

    // ------------------------------------------------------------------------------------------------ accessors for tests

    static SlotAllocator.Result result() {
        return result;
    }
}
