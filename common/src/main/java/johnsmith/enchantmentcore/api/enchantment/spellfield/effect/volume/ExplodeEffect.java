package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.volume;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.effect.ExplosionDefinition;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.ProbabilityDistribution;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVolumeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.util.SpellFieldTaskScheduler;

import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * Dual-purpose spell field effect implementing both {@link SpellFieldEntityEffect} and
 * {@link SpellFieldVolumeEffect} that schedules one or more explosions configured by an
 * {@link ExplosionDefinition}.
 * <p>
 * Supports base fuse delays, randomized fuse variance, and distance-proportional spatial wave delays
 * ($\text{distanceFromEpicenter} \cdot \text{spatialDelay}$) scheduled via {@link SpellFieldTaskScheduler}.
 *
 * @param explosion    The {@link ExplosionDefinition} governing radius, block interaction, fire, and particles.
 * @param count        The {@link LevelBasedValue} number of explosions triggered when evaluated as a volume effect.
 * @param fuseTicks    The {@link LevelBasedValue} baseline delay in ticks before detonation.
 * @param fuseVariance The {@link LevelBasedValue} random +/- tick jitter applied to the fuse delay.
 * @param spatialDelay The {@link LevelBasedValue} additional delay in ticks per block of distance from the spell field epicenter.
 * @param distribution Optional {@link ProbabilityDistribution} used to sample detonation points when evaluated as a volume effect.
 * @param entityFilter Optional {@link ContextAwarePredicate} restricting which entities trigger explosions when evaluated as an entity effect.
 */
public record ExplodeEffect(
        ExplosionDefinition explosion,
        LevelBasedValue count,
        LevelBasedValue fuseTicks,
        LevelBasedValue fuseVariance,
        LevelBasedValue spatialDelay,
        Optional<ProbabilityDistribution> distribution,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect, SpellFieldVolumeEffect {
    /** JSON field key for the nested explosion definition. */
    public static final String EXPLOSION = "explosion";

    /** JSON field key for the volume explosion count. */
    public static final String COUNT = "count";

    /** JSON field key for the base fuse delay in ticks. */
    public static final String FUSE_TICKS = "fuse_ticks";

    /** JSON field key for the random fuse variance in ticks. */
    public static final String FUSE_VARIANCE = "fuse_variance";

    /** JSON field key for the per-block distance delay in ticks. */
    public static final String SPATIAL_DELAY = "spatial_delay";

    /** JSON field key for the optional spatial probability distribution. */
    public static final String DISTRIBUTION = "distribution";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field effect. */
    public static final String KEY = "explode";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> {
        Set<String> set = new HashSet<>(Set.of(COUNT, FUSE_TICKS, FUSE_VARIANCE, SPATIAL_DELAY));
        set.addAll(ExplosionDefinition.KEY_PROVIDER.getLevelBasedKeys());
        return set;
    };

    /**
     * MapCodec for serializing and deserializing {@link ExplodeEffect} instances from JSON.
     */
    public static final MapCodec<ExplodeEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExplosionDefinition.CODEC.fieldOf(EXPLOSION).forGetter(ExplodeEffect::explosion),
            LevelBasedValue.CODEC.optionalFieldOf(COUNT, LevelBasedValue.constant(1.0F)).forGetter(ExplodeEffect::count),
            LevelBasedValue.CODEC.optionalFieldOf(FUSE_TICKS, LevelBasedValue.constant(0.0F)).forGetter(ExplodeEffect::fuseTicks),
            LevelBasedValue.CODEC.optionalFieldOf(FUSE_VARIANCE, LevelBasedValue.constant(0.0F)).forGetter(ExplodeEffect::fuseVariance),
            LevelBasedValue.CODEC.optionalFieldOf(SPATIAL_DELAY, LevelBasedValue.constant(0.0F)).forGetter(ExplodeEffect::spatialDelay),
            ProbabilityDistribution.CODEC.optionalFieldOf(DISTRIBUTION).forGetter(ExplodeEffect::distribution),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(ExplodeEffect::entityFilter)
    ).apply(instance, ExplodeEffect::new));

    /**
     * Schedules an explosion centered at {@code victim}'s position with its blast radius scaled by
     * {@code volumeScalar} if the entity matches {@link #entityFilter()}.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param victim           The target entity intersecting the spell field.
     * @param volumeScalar     The active topological multiplier for {@code victim}.
     */
    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;
        this.dispatchExplosion(level, enchantmentLevel, context.owner(), victim.position(), epicenter, volumeScalar);
    }

    /**
     * Samples {@link #count()} points across {@code volumes} using {@link #distribution()} (or uniform
     * sampling if absent) and schedules an explosion at each sampled coordinate.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param volumes          The list of evaluated world-space {@link GlobalVolume}s defining the spell field geometry.
     */
    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, List<GlobalVolume> volumes) {
        if (volumes.isEmpty()) return;

        int totalExplosions = (int) this.count.calculate(enchantmentLevel);

        for (int i = 0; i < totalExplosions; i++) {
            Vec3 targetPoint = this.distribution
                    .map(d -> d.samplePoint(level, enchantmentLevel, spatialReference, epicenter, volumes))
                    .orElseGet(() -> ProbabilityDistribution.getUniformPoint(volumes, level.random));
            this.dispatchExplosion(level, enchantmentLevel, context.owner(), targetPoint, epicenter, 1.0F);
        }
    }

    /**
     * Computes the total fuse delay (combining {@link #fuseTicks()}, {@link #spatialDelay()}, and
     * {@link #fuseVariance()}) and schedules the detonation with {@link SpellFieldTaskScheduler}.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param owner            The entity attributed as the explosion source, or {@code null}.
     * @param targetPos        The world-space detonation coordinate.
     * @param epicenter        The spell field epicenter used for spatial delay distance calculation.
     * @param scalar           The topological radius multiplier scalar.
     */
    private void dispatchExplosion(ServerLevel level, int enchantmentLevel, Entity owner, Vec3 targetPos, Vec3 epicenter, float scalar) {
        if (scalar <= 0.0001F) return;

        int baseFuse = (int) this.fuseTicks.calculate(enchantmentLevel);
        int variance = (int) this.fuseVariance.calculate(enchantmentLevel);
        float distance = (float) targetPos.distanceTo(epicenter);
        int spatialOffset = (int) (distance * this.spatialDelay.calculate(enchantmentLevel));

        int fuse = baseFuse + spatialOffset + (variance > 0 ? level.random.nextInt(variance * 2) - variance : 0);
        fuse = Math.max(0, fuse);

        Runnable explodeAction = () -> this.explosion.explode(level, enchantmentLevel, owner, targetPos, scalar);
        SpellFieldTaskScheduler.schedule(level, fuse, explodeAction);
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link ExplodeEffect} with uniform volume sampling.
     *
     * @param explosion    The explosion parameters definition.
     * @param count        The number of volume explosions.
     * @param fuseTicks    The base fuse delay in ticks.
     * @param fuseVariance The random fuse variance in ticks.
     * @param spatialDelay The per-block spatial delay in ticks.
     */
    public ExplodeEffect(ExplosionDefinition explosion, LevelBasedValue count, LevelBasedValue fuseTicks, LevelBasedValue fuseVariance, LevelBasedValue spatialDelay) {
        this(explosion, count, fuseTicks, fuseVariance, spatialDelay, Optional.empty(), Optional.empty());
    }

    /**
     * Constructs an unfiltered {@link ExplodeEffect} with a custom spatial {@link ProbabilityDistribution}.
     *
     * @param explosion    The explosion parameters definition.
     * @param count        The number of volume explosions.
     * @param fuseTicks    The base fuse delay in ticks.
     * @param fuseVariance The random fuse variance in ticks.
     * @param spatialDelay The per-block spatial delay in ticks.
     * @param distribution The spatial probability distribution for volume sampling.
     */
    public ExplodeEffect(ExplosionDefinition explosion, LevelBasedValue count, LevelBasedValue fuseTicks, LevelBasedValue fuseVariance, LevelBasedValue spatialDelay, ProbabilityDistribution distribution) {
        this(explosion, count, fuseTicks, fuseVariance, spatialDelay, Optional.of(distribution), Optional.empty());
    }

    /**
     * Constructs an {@link ExplodeEffect} restricted by an entity predicate filter.
     *
     * @param explosion    The explosion parameters definition.
     * @param count        The number of volume explosions.
     * @param fuseTicks    The base fuse delay in ticks.
     * @param fuseVariance The random fuse variance in ticks.
     * @param spatialDelay The per-block spatial delay in ticks.
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public ExplodeEffect(ExplosionDefinition explosion, LevelBasedValue count, LevelBasedValue fuseTicks, LevelBasedValue fuseVariance, LevelBasedValue spatialDelay, ContextAwarePredicate entityFilter) {
        this(explosion, count, fuseTicks, fuseVariance, spatialDelay, Optional.empty(), Optional.of(entityFilter));
    }

    /**
     * Constructs an {@link ExplodeEffect} with both a custom {@link ProbabilityDistribution} and an entity filter.
     *
     * @param explosion    The explosion parameters definition.
     * @param count        The number of volume explosions.
     * @param fuseTicks    The base fuse delay in ticks.
     * @param fuseVariance The random fuse variance in ticks.
     * @param spatialDelay The per-block spatial delay in ticks.
     * @param distribution The spatial probability distribution for volume sampling.
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public ExplodeEffect(ExplosionDefinition explosion, LevelBasedValue count, LevelBasedValue fuseTicks, LevelBasedValue fuseVariance, LevelBasedValue spatialDelay, ProbabilityDistribution distribution, ContextAwarePredicate entityFilter) {
        this(explosion, count, fuseTicks, fuseVariance, spatialDelay, Optional.of(distribution), Optional.of(entityFilter));
    }
    // endregion
}