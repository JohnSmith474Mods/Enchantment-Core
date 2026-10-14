package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.visual;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.ProbabilityDistribution;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVisualEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldAxis;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldVisualEffect} that spawns ambient particles with random omnidirectional drift
 * velocities at coordinates sampled across the spell field's {@link GlobalVolume}s via a
 * {@link ProbabilityDistribution}.
 *
 * @param particle     The {@link ParticleOptions} defining the particle type to spawn.
 * @param count        The {@link LevelBasedValue} total number of particles spawned per evaluation cycle.
 * @param speed        The {@link LevelBasedValue} speed multiplier applied to each particle's random velocity vector.
 * @param distribution The {@link ProbabilityDistribution} used to sample topological spawn coordinates across the volumes.
 */
public record AmbientFieldVisualEffect(
        ParticleOptions particle,
        LevelBasedValue count,
        LevelBasedValue speed,
        ProbabilityDistribution distribution
) implements SpellFieldVisualEffect {
    /** JSON field key for the particle options definition. */
    public static final String PARTICLE = "particle";

    /** JSON field key for the particle count. */
    public static final String COUNT = "count";

    /** JSON field key for the particle speed multiplier. */
    public static final String SPEED = "speed";

    /** JSON field key for the spatial probability distribution. */
    public static final String DISTRIBUTION = "distribution";

    /** Registry path identifier for this spell field visual effect. */
    public static final String KEY = "ambient_field";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(COUNT, SPEED);

    /**
     * MapCodec for serializing and deserializing {@link AmbientFieldVisualEffect} instances from JSON.
     */
    public static final MapCodec<AmbientFieldVisualEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ParticleTypes.CODEC.fieldOf(PARTICLE).forGetter(AmbientFieldVisualEffect::particle),
            LevelBasedValue.CODEC.fieldOf(COUNT).forGetter(AmbientFieldVisualEffect::count),
            LevelBasedValue.CODEC.fieldOf(SPEED).forGetter(AmbientFieldVisualEffect::speed),
            ProbabilityDistribution.CODEC.fieldOf(DISTRIBUTION).forGetter(AmbientFieldVisualEffect::distribution)
    ).apply(instance, AmbientFieldVisualEffect::new));

    /**
     * Samples {@link #count()} points across {@code volumes} using {@link #distribution()} and broadcasts
     * single-particle packets (`count = 0`) with randomized velocity vectors scaled by {@link #speed()}.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param target           The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param volumes          The list of evaluated world-space {@link GlobalVolume}s defining the spell field geometry.
     * @param activeAxes       The list of active directional {@link FieldAxis} instances collected from sibling effects.
     */
    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Vec3 epicenter, List<GlobalVolume> volumes, List<FieldAxis> activeAxes) {
        int particleCount = (int) this.count.calculate(enchantmentLevel);
        float particleSpeed = this.speed.calculate(enchantmentLevel);

        for (int i = 0; i < particleCount; i++) {
            Vec3 spawnPos = this.distribution.samplePoint(level, enchantmentLevel, target, epicenter, volumes);
            double vx = (level.random.nextDouble() - 0.5) * 2.0;
            double vy = (level.random.nextDouble() - 0.5) * 2.0;
            double vz = (level.random.nextDouble() - 0.5) * 2.0;

            level.sendParticles(this.particle, spawnPos.x, spawnPos.y, spawnPos.z, 0, vx, vy, vz, particleSpeed);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldVisualEffect> codec() { return CODEC; }
}