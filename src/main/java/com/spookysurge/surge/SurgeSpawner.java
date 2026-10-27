package com.spookysurge.surge;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.spookysurge.config.SurgeConfig;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import kotlin.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Creates the wild Pokemon of a surge. It does not use Cobblemon's spawner: each one is built and sent
 * out directly, tagged in the Pokemon's own persistent data so the tag survives a capture.
 */
public final class SurgeSpawner {
    public static final String TAG_SURGE = "spookysurge";
    public static final String TAG_RARITY = "spookysurge_rarity";

    private SurgeSpawner() {}

    /** The entity's UUID is passed to {@code onCreated} before the entity is added to the world, so it can be tracked first. */
    public interface Created {
        void accept(UUID entityId);
    }

    public static Optional<BlockPos> findPosition(ServerLevel level, ServerPlayer player, SurgeConfig config, Random random) {
        BlockPos center = player.blockPosition();
        double min = config.minSpawnDistance;
        double max = config.maxSpawnDistance;
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = Math.sqrt(min * min + random.nextDouble() * (max * max - min * min));
            int x = center.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * radius);
            if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (!level.getWorldBorder().isWithinBounds(candidate)) continue;
            if (!isSimpleSurface(level, candidate)) continue;
            return Optional.of(candidate);
        }
        return Optional.empty();
    }

    private static boolean isSimpleSurface(ServerLevel level, BlockPos pos) {
        var ground = level.getBlockState(pos.below());
        var feet = level.getBlockState(pos);
        var head = level.getBlockState(pos.above());
        if (ground.isAir() || ground.getCollisionShape(level, pos.below()).isEmpty()) return false;
        if (!feet.getFluidState().isEmpty() || !head.getFluidState().isEmpty()) return false;
        return feet.getCollisionShape(level, pos).isEmpty() && head.getCollisionShape(level, pos.above()).isEmpty();
    }

    /**
     * Spawns one surge Pokemon. Returns the entity, or empty if the species is unknown (a typo in the
     * config must cost one spawn, not the server).
     */
    public static Optional<PokemonEntity> spawn(ServerLevel level, BlockPos pos, String speciesId, String rarity,
                                                String surgeId, boolean boostedShiny, SurgeConfig config,
                                                Random random, Created onCreated) {
        Species species = PokemonSpecies.getByIdentifier(ResourceLocation.fromNamespaceAndPath("cobblemon", speciesId));
        if (species == null) return Optional.empty();

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setLevel(config.levelMin + random.nextInt(config.levelMax - config.levelMin + 1));
        pokemon.initializeMoveset(false);
        pokemon.setCurrentHealth(pokemon.getMaxHealth());
        float baseRate = Cobblemon.INSTANCE.getConfig().getShinyRate();
        double odds = baseRate <= 0 ? 0 : (1.0 / baseRate) * (boostedShiny ? config.shinyMultiplier : 1.0);
        if (random.nextDouble() < odds) pokemon.setShiny(true);
        pokemon.getPersistentData().putString(TAG_SURGE, surgeId);
        pokemon.getPersistentData().putString(TAG_RARITY, rarity);

        Vec3 at = Vec3.atBottomCenterOf(pos);
        PokemonEntity entity = pokemon.sendOut(level, at, null, spawned -> {
            spawned.setCountsTowardsSpawnCap(false);
            onCreated.accept(spawned.getUUID());
            return Unit.INSTANCE;
        });
        if (entity != null) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 0.8, at.z, 24, 0.4, 0.6, 0.4, 0.02);
        }
        return Optional.ofNullable(entity);
    }

    /** Whether Cobblemon knows a species with this id. Used at startup to catch config typos. */
    public static boolean speciesExists(String speciesId) {
        try {
            return PokemonSpecies.getByIdentifier(ResourceLocation.fromNamespaceAndPath("cobblemon", speciesId)) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** True if this Pokemon carries a surge tag. */
    public static boolean isSurge(Pokemon pokemon) {
        return pokemon.getPersistentData().contains(TAG_SURGE);
    }
}
