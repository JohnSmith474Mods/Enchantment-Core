package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.volume;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.ProbabilityDistribution;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVolumeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.util.SpellFieldTaskScheduler;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * Dual-purpose spell field effect implementing both {@link SpellFieldEntityEffect} and
 * {@link SpellFieldVolumeEffect} that schedules one or more {@link LightningBolt} strikes at
 * targeted entities or sampled volume coordinates.
 *
 * @param count         The {@link LevelBasedValue} number of lightning bolts to strike (supports fractional probabilistic rounding).
 * @param delayTicks    The {@link LevelBasedValue} base delay in ticks before each lightning bolt strikes.
 * @param delayVariance The {@link LevelBasedValue} random +/- tick jitter applied to each strike's delay.
 * @param visualOnly    Whether the spawned lightning bolts are purely visual ({@link LightningBolt#setVisualOnly(boolean)}) without dealing damage or starting fires.
 * @param distribution  Optional {@link ProbabilityDistribution} used to sample strike coordinates when evaluated as a volume effect.
 * @param entityFilter  Optional {@link ContextAwarePredicate} restricting which entities are struck when evaluated as an entity effect.
 */
public record LightningStrikeEffect(
        LevelBasedValue count,
        LevelBasedValue delayTicks,
        LevelBasedValue delayVariance,
        boolean visualOnly,
        Optional<ProbabilityDistribution> distribution,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect, SpellFieldVolumeEffect {
    /** JSON field key for the lightning strike count. */
    public static final String COUNT = "count";

    /** JSON field key for the base strike delay in ticks. */
    public static final String DELAY_TICKS = "delay_ticks";

    /** JSON field key for the random delay variance in ticks. */
    public static final String DELAY_VARIANCE = "delay_variance";

    /** JSON field key for toggling visual-only lightning bolts. */
    public static final String VISUAL_ONLY = "visual_only";

    /** JSON field key for the optional spatial probability distribution. */
    public static final String DISTRIBUTION = "distribution";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field effect. */
    public static final String KEY = "lightning_strike";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(COUNT, DELAY_TICKS, DELAY_VARIANCE);

    /**
     * MapCodec for serializing and deserializing {@link LightningStrikeEffect} instances from JSON.
     */
    public static final MapCodec<LightningStrikeEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.optionalFieldOf(COUNT, LevelBasedValue.constant(1.0F)).forGetter(LightningStrikeEffect::count),
            LevelBasedValue.CODEC.optionalFieldOf(DELAY_TICKS, LevelBasedValue.constant(0.0F)).forGetter(LightningStrikeEffect::delayTicks),
            LevelBasedValue.CODEC.optionalFieldOf(DELAY_VARIANCE, LevelBasedValue.constant(0.0F)).forGetter(LightningStrikeEffect::delayVariance),
            Codec.BOOL.optionalFieldOf(VISUAL_ONLY, false).forGetter(LightningStrikeEffect::visualOnly),
            ProbabilityDistribution.CODEC.optionalFieldOf(DISTRIBUTION).forGetter(LightningStrikeEffect::distribution),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(LightningStrikeEffect::entityFilter)
    ).apply(instance, LightningStrikeEffect::new));

    /**
     * Schedules lightning strikes at {@code victim}'s position with the strike count scaled by
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
        this.dispatchStrikes(level, enchantmentLevel, victim.position(), volumeScalar);
    }

    /**
     * Samples a target coordinate across {@code volumes} using {@link #distribution()} (or uniform
     * sampling if absent) and schedules the configured number of lightning strikes at that location.
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

        Vec3 targetPoint = this.distribution
                .map(d -> d.samplePoint(level, enchantmentLevel, spatialReference, epicenter, volumes))
                .orElseGet(() -> ProbabilityDistribution.getUniformPoint(volumes, level.random));
        this.dispatchStrikes(level, enchantmentLevel, targetPoint, 1.0F);
    }

    /**
     * Calculates the probabilistically rounded number of strikes and schedules each {@link LightningBolt}
     * spawn via {@link SpellFieldTaskScheduler}.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param targetPos        The world-space strike coordinate.
     * @param scalar           The topological count multiplier scalar.
     */
    private void dispatchStrikes(ServerLevel level, int enchantmentLevel, Vec3 targetPos, float scalar) {
        if (scalar <= 0.0001F) return;

        float calculatedCount = this.count.calculate(enchantmentLevel) * scalar;
        int totalStrikes = (int) calculatedCount;
        if (level.random.nextFloat() < (calculatedCount - totalStrikes)) {
            totalStrikes++;
        }
        if (totalStrikes <= 0) return;

        int baseDelay = (int) this.delayTicks.calculate(enchantmentLevel);
        int variance = (int) this.delayVariance.calculate(enchantmentLevel);

        for (int i = 0; i < totalStrikes; i++) {
            int timeOffset = baseDelay + (variance > 0 ? level.random.nextInt(variance * 2) - variance : 0);
            timeOffset = Math.max(0, timeOffset);

            Runnable strikeAction = () -> {
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                if (lightning != null) {
                    lightning.snapTo(targetPos);
                    lightning.setVisualOnly(this.visualOnly);
                    level.addFreshEntity(lightning);
                }
            };

            SpellFieldTaskScheduler.schedule(level, timeOffset, strikeAction);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link LightningStrikeEffect} with uniform volume sampling.
     *
     * @param count         The number of lightning strikes.
     * @param delayTicks    The base delay in ticks.
     * @param delayVariance The random delay variance in ticks.
     * @param visualOnly    Whether the lightning bolts are visual-only.
     */
    public LightningStrikeEffect(LevelBasedValue count, LevelBasedValue delayTicks, LevelBasedValue delayVariance, boolean visualOnly) {
        this(count, delayTicks, delayVariance, visualOnly, Optional.empty(), Optional.empty());
    }

    /**
     * Constructs an unfiltered {@link LightningStrikeEffect} with a custom spatial {@link ProbabilityDistribution}.
     *
     * @param count         The number of lightning strikes.
     * @param delayTicks    The base delay in ticks.
     * @param delayVariance The random delay variance in ticks.
     * @param visualOnly    Whether the lightning bolts are visual-only.
     * @param distribution  The spatial probability distribution for volume sampling.
     */
    public LightningStrikeEffect(LevelBasedValue count, LevelBasedValue delayTicks, LevelBasedValue delayVariance, boolean visualOnly, ProbabilityDistribution distribution) {
        this(count, delayTicks, delayVariance, visualOnly, Optional.of(distribution), Optional.empty());
    }

    /**
     * Constructs a {@link LightningStrikeEffect} restricted by an entity predicate filter.
     *
     * @param count         The number of lightning strikes.
     * @param delayTicks    The base delay in ticks.
     * @param delayVariance The random delay variance in ticks.
     * @param visualOnly    Whether the lightning bolts are visual-only.
     * @param entityFilter  The predicate filter to match against candidate entities.
     */
    public LightningStrikeEffect(LevelBasedValue count, LevelBasedValue delayTicks, LevelBasedValue delayVariance, boolean visualOnly, ContextAwarePredicate entityFilter) {
        this(count, delayTicks, delayVariance, visualOnly, Optional.empty(), Optional.of(entityFilter));
    }

    /**
     * Constructs a {@link LightningStrikeEffect} with both a custom {@link ProbabilityDistribution} and an entity filter.
     *
     * @param count         The number of lightning strikes.
     * @param delayTicks    The base delay in ticks.
     * @param delayVariance The random delay variance in ticks.
     * @param visualOnly    Whether the lightning bolts are visual-only.
     * @param distribution  The spatial probability distribution for volume sampling.
     * @param entityFilter  The predicate filter to match against candidate entities.
     */
    public LightningStrikeEffect(LevelBasedValue count, LevelBasedValue delayTicks, LevelBasedValue delayVariance, boolean visualOnly, ProbabilityDistribution distribution, ContextAwarePredicate entityFilter) {
        this(count, delayTicks, delayVariance, visualOnly, Optional.of(distribution), Optional.of(entityFilter));
    }
    // endregion
}