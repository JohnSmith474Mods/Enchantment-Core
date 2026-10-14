package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.visual;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;
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
 * A {@link SpellFieldVisualEffect} that spawns directional particles across the spell field's
 * {@link GlobalVolume}s whose velocity vectors align with a configured {@link FieldAxis} or
 * automatically inherit the active axes of sibling {@link johnsmith.enchantmentcore.api.enchantment.spellfield.effect.DirectionalSpellFieldEffect}s.
 *
 * @param particle     The {@link ParticleOptions} defining the particle type to spawn.
 * @param count        The {@link LevelBasedValue} total number of particles spawned per evaluation cycle.
 * @param speed        The {@link LevelBasedValue} signed speed multiplier (negative values invert the directional polarity).
 * @param axis         Optional explicit {@link FieldAxis} overriding automatic axis inheritance from sibling effects.
 * @param distribution The {@link ProbabilityDistribution} used to sample topological spawn coordinates across the volumes.
 */
public record VectorFieldVisualEffect(
        ParticleOptions particle,
        LevelBasedValue count,
        LevelBasedValue speed,
        Optional<FieldAxis> axis,
        ProbabilityDistribution distribution
) implements SpellFieldVisualEffect {
    /** JSON field key for the particle options definition. */
    public static final String PARTICLE = "particle";

    /** JSON field key for the particle count. */
    public static final String COUNT = "count";

    /** JSON field key for the signed particle speed multiplier. */
    public static final String SPEED = "speed";

    /** JSON field key for the optional explicit directional axis. */
    public static final String AXIS = "axis";

    /** JSON field key for the spatial probability distribution. */
    public static final String DISTRIBUTION = "distribution";

    /** Registry path identifier for this spell field visual effect. */
    public static final String KEY = "vector_field";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(COUNT, SPEED);

    /**
     * MapCodec for serializing and deserializing {@link VectorFieldVisualEffect} instances from JSON.
     */
    public static final MapCodec<VectorFieldVisualEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ParticleTypes.CODEC.fieldOf(PARTICLE).forGetter(VectorFieldVisualEffect::particle),
            LevelBasedValue.CODEC.fieldOf(COUNT).forGetter(VectorFieldVisualEffect::count),
            LevelBasedValue.CODEC.fieldOf(SPEED).forGetter(VectorFieldVisualEffect::speed),
            FieldAxis.CODEC.optionalFieldOf(AXIS).forGetter(VectorFieldVisualEffect::axis),
            ProbabilityDistribution.CODEC.fieldOf(DISTRIBUTION).forGetter(VectorFieldVisualEffect::distribution)
    ).apply(instance, VectorFieldVisualEffect::new));

    /**
     * Samples {@link #count()} points across {@code volumes} and spawns velocity-aligned particles
     * along {@link #axis()} (or a randomly chosen axis from {@code activeAxes}, falling back to
     * random omnidirectional drift if no axis is available).
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
        float configuredSpeed = this.speed.calculate(enchantmentLevel);
        float polarity = configuredSpeed < 0 ? -1.0F : 1.0F;
        float particleSpeed = Math.abs(configuredSpeed);

        int particleCount = (int) this.count.calculate(enchantmentLevel);

        for (int i = 0; i < particleCount; i++) {
            Vec3 spawnPos = this.distribution.samplePoint(level, enchantmentLevel, target, epicenter, volumes);
            FieldAxis resolvedAxis = this.axis.orElseGet(() -> activeAxes.isEmpty() ? null : activeAxes.get(level.getRandom().nextInt(activeAxes.size())));
            double vx, vy, vz;

            if (resolvedAxis != null) {
                Vec3 velocity = resolvedAxis.getImpulseVector(target, epicenter, spawnPos).normalize().scale(polarity);
                vx = velocity.x;
                vy = velocity.y;
                vz = velocity.z;
            } else {
                vx = (level.getRandom().nextDouble() - 0.5) * 2.0;
                vy = (level.getRandom().nextDouble() - 0.5) * 2.0;
                vz = (level.getRandom().nextDouble() - 0.5) * 2.0;
            }

            level.sendParticles(this.particle, spawnPos.x, spawnPos.y, spawnPos.z, 0, vx, vy, vz, particleSpeed);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldVisualEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs a {@link VectorFieldVisualEffect} that automatically inherits its directional axis
     * from sibling effects in the spell field.
     *
     * @param particle     The particle options to spawn.
     * @param count        The {@link LevelBasedValue} particle count.
     * @param speed        The {@link LevelBasedValue} signed particle speed.
     * @param distribution The spatial probability distribution for sampling spawn points.
     */
    public VectorFieldVisualEffect(ParticleOptions particle, LevelBasedValue count, LevelBasedValue speed, ProbabilityDistribution distribution) {
        this(particle, count, speed, Optional.empty(), distribution);
    }

    /**
     * Constructs a {@link VectorFieldVisualEffect} aligned with an explicit {@link FieldAxis}.
     *
     * @param particle     The particle options to spawn.
     * @param count        The {@link LevelBasedValue} particle count.
     * @param speed        The {@link LevelBasedValue} signed particle speed.
     * @param axis         The explicit {@link FieldAxis} dictating particle velocity direction.
     * @param distribution The spatial probability distribution for sampling spawn points.
     */
    public VectorFieldVisualEffect(ParticleOptions particle, LevelBasedValue count, LevelBasedValue speed, FieldAxis axis, ProbabilityDistribution distribution) {
        this(particle, count, speed, Optional.of(axis), distribution);
    }
    // endregion
}