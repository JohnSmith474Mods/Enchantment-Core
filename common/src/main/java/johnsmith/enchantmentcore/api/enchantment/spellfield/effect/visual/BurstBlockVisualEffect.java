package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.visual;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.TopologyEvaluator;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A visual {@link SpellFieldBlockEffect} that emits a localized burst of particles centered at each
 * evaluated block coordinate that passes its {@link TopologyEvaluator}, scaling both particle count
 * and speed by the block's topological multiplier.
 *
 * @param particle  The {@link ParticleOptions} defining the particle type to emit.
 * @param count     The {@link LevelBasedValue} base number of particles emitted per block before topological scaling.
 * @param speed     The {@link LevelBasedValue} base particle speed before topological scaling.
 * @param evaluator The {@link TopologyEvaluator} governing deterministic threshold or probabilistic activation.
 */
public record BurstBlockVisualEffect(
        ParticleOptions particle,
        LevelBasedValue count,
        LevelBasedValue speed,
        TopologyEvaluator evaluator
) implements SpellFieldBlockEffect {
    /** JSON field key for the particle options definition. */
    public static final String PARTICLE = "particle";

    /** JSON field key for the base particle count. */
    public static final String COUNT = "count";

    /** JSON field key for the base particle speed. */
    public static final String SPEED = "speed";

    /** JSON field key for the topological threshold/probability evaluator. */
    public static final String EVALUATOR = "evaluator";

    /** Registry path identifier for this spell field block effect. */
    public static final String KEY = "burst_block";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(COUNT, SPEED);

    /**
     * MapCodec for serializing and deserializing {@link BurstBlockVisualEffect} instances from JSON.
     */
    public static final MapCodec<BurstBlockVisualEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ParticleTypes.CODEC.fieldOf(PARTICLE).forGetter(BurstBlockVisualEffect::particle),
            LevelBasedValue.CODEC.fieldOf(COUNT).forGetter(BurstBlockVisualEffect::count),
            LevelBasedValue.CODEC.fieldOf(SPEED).forGetter(BurstBlockVisualEffect::speed),
            TopologyEvaluator.CODEC.optionalFieldOf(EVALUATOR, TopologyEvaluator.PROBABILISTIC).forGetter(BurstBlockVisualEffect::evaluator)
    ).apply(instance, BurstBlockVisualEffect::new));

    /**
     * Returns the execution priority of this block effect derived from the evaluator's threshold.
     *
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @return The priority scalar.
     */
    @Override
    public float getPriority(int enchantmentLevel) {
        return this.evaluator.getThreshold(enchantmentLevel);
    }

    /**
     * Broadcasts a particle burst around the center of {@code pos} with a $0.5$ block spread along
     * each axis when permitted by {@link #evaluator()}.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param pos              The target block coordinate.
     * @param scalar           The active topological multiplier at {@code pos}.
     * @param modifiedBlocks   A mutable set tracking block coordinates modified during this evaluation cycle.
     */
    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, BlockPos pos, float scalar, Set<BlockPos> modifiedBlocks) {
        if (!this.evaluator.evaluate(scalar, enchantmentLevel, level.random)) return;

        int particleCount = (int) (this.count.calculate(enchantmentLevel) * scalar);
        if (particleCount <= 0) return;

        float particleSpeed = this.speed.calculate(enchantmentLevel) * scalar;
        Vec3 center = Vec3.atCenterOf(pos);

        level.sendParticles(this.particle, center.x, center.y, center.z, particleCount, 0.5, 0.5, 0.5, particleSpeed);
    }

    @Override
    public MapCodec<? extends SpellFieldBlockEffect> codec() { return CODEC; }

    // region Convenience Constructors

    /**
     * Constructs a {@link BurstBlockVisualEffect} using the default {@link TopologyEvaluator#PROBABILISTIC} evaluator.
     *
     * @param particle The particle options to emit.
     * @param count    The {@link LevelBasedValue} base particle count.
     * @param speed    The {@link LevelBasedValue} base particle speed.
     */
    public BurstBlockVisualEffect(ParticleOptions particle, LevelBasedValue count, LevelBasedValue speed) {
        this(particle, count, speed, TopologyEvaluator.PROBABILISTIC);
    }
    // endregion
}