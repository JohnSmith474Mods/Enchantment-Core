package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

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

import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * Dual-purpose spell field effect that summons one or more entities of a configured {@link EntityType}.
 * <p>
 * Implements both {@link SpellFieldEntityEffect} and {@link SpellFieldVolumeEffect} so it can be
 * used in either {@code "entity_effects"} (spawning at each targeted entity's location) or
 * {@code "volume_effects"} (spawning at points sampled across the volume geometry, including
 * self-targeted spell fields where {@code context.owner()} is excluded from {@code entity_effects}).
 *
 * @param entityType   The {@link EntityType} to instantiate and spawn.
 * @param count        The {@link LevelBasedValue} number of entities to spawn (supports fractional probabilistic rounding).
 * @param distribution Optional {@link ProbabilityDistribution} used to sample spawn coordinates when evaluated as a volume effect.
 * @param entityFilter Optional {@link ContextAwarePredicate} restricting target eligibility.
 */
public record SummonEntityEffect(
        EntityType<?> entityType,
        LevelBasedValue count,
        Optional<ProbabilityDistribution> distribution,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect, SpellFieldVolumeEffect {
    /** JSON field key for the entity type identifier. */
    public static final String ENTITY_TYPE = "entity_type";

    /** JSON field key for the number of entities to summon. */
    public static final String COUNT = "count";

    /** JSON field key for the optional spatial probability distribution. */
    public static final String DISTRIBUTION = "distribution";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field effect. */
    public static final String KEY = "summon_entity";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(COUNT);

    /**
     * MapCodec for serializing and deserializing {@link SummonEntityEffect} instances from JSON.
     */
    public static final MapCodec<SummonEntityEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf(ENTITY_TYPE).forGetter(SummonEntityEffect::entityType),
            LevelBasedValue.CODEC.optionalFieldOf(COUNT, LevelBasedValue.constant(1.0F)).forGetter(SummonEntityEffect::count),
            ProbabilityDistribution.CODEC.optionalFieldOf(DISTRIBUTION).forGetter(SummonEntityEffect::distribution),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(SummonEntityEffect::entityFilter)
    ).apply(instance, SummonEntityEffect::new));

    /**
     * Constructs a {@link SummonEntityEffect} without a custom spatial distribution.
     *
     * @param entityType   The {@link EntityType} to summon.
     * @param count        The {@link LevelBasedValue} spawn count.
     * @param entityFilter Optional entity predicate filter.
     */
    public SummonEntityEffect(
            EntityType<?> entityType,
            LevelBasedValue count,
            Optional<ContextAwarePredicate> entityFilter
    ) {
        this(entityType, count, Optional.empty(), entityFilter);
    }

    /**
     * Summons the calculated number of entities directly at {@code victim}'s position and orientation.
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
    public void apply(
            ServerLevel level,
            int enchantmentLevel,
            EnchantedItemInUse context,
            Entity spatialReference,
            Vec3 epicenter,
            Entity victim,
            float volumeScalar
    ) {
        if (this.entityFilter.isPresent()
                && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) {
            return;
        }

        int spawnAmount = this.calculateSpawnAmount(level, enchantmentLevel, volumeScalar);
        for (int i = 0; i < spawnAmount; i++) {
            this.spawnSingleEntity(
                    level,
                    victim.position(),
                    victim.getYRot(),
                    victim.getXRot(),
                    context.owner()
            );
        }
    }

    /**
     * Summons the calculated number of entities at coordinates sampled across {@code volumes}
     * using {@link #distribution()} (or uniform volume sampling if absent).
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param volumes          The list of evaluated world-space {@link GlobalVolume}s defining the spell field geometry.
     */
    @Override
    public void apply(
            ServerLevel level,
            int enchantmentLevel,
            EnchantedItemInUse context,
            Entity spatialReference,
            Vec3 epicenter,
            List<GlobalVolume> volumes
    ) {
        if (volumes.isEmpty()) {
            return;
        }

        Entity filterTarget = spatialReference != null ? spatialReference : context.owner();
        if (this.entityFilter.isPresent()
                && filterTarget != null
                && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, filterTarget, enchantmentLevel))) {
            return;
        }

        int spawnAmount = this.calculateSpawnAmount(level, enchantmentLevel, 1.0F);
        if (spawnAmount <= 0) {
            return;
        }

        Entity rotationRef = spatialReference != null ? spatialReference : context.owner();
        float yRot = rotationRef != null ? rotationRef.getYRot() : level.random.nextFloat() * 360.0F;
        float xRot = rotationRef != null ? rotationRef.getXRot() : 0.0F;

        for (int i = 0; i < spawnAmount; i++) {
            Vec3 targetPoint = this.distribution
                    .map(d -> d.samplePoint(level, enchantmentLevel, spatialReference, epicenter, volumes))
                    .orElseGet(() -> ProbabilityDistribution.getUniformPoint(volumes, level.random));
            this.spawnSingleEntity(level, targetPoint, yRot, xRot, context.owner());
        }
    }

    /**
     * Computes the integer number of entities to spawn, treating any fractional remainder of
     * $\text{count} \cdot \text{scalar}$ as a probability for one additional spawn.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param scalar           The topological multiplier scalar.
     * @return The non-negative integer number of entities to summon.
     */
    private int calculateSpawnAmount(ServerLevel level, int enchantmentLevel, float scalar) {
        if (scalar <= 0.0001F) {
            return 0;
        }

        float calculatedCount = this.count.calculate(enchantmentLevel) * scalar;
        int spawnAmount = (int) calculatedCount;
        if (level.random.nextFloat() < (calculatedCount - spawnAmount)) {
            spawnAmount++;
        }
        return spawnAmount;
    }

    /**
     * Instantiates a single entity of {@link #entityType()}, positions and orients it, finalizes
     * mob spawn data if it is a {@link Mob}, and assigns {@code owner} if it is a {@link Projectile}
     * or {@link EvokerFangs}.
     *
     * @param level The server level in which to spawn the entity.
     * @param pos   The world-space spawn coordinates.
     * @param yRot  The initial yaw rotation in degrees.
     * @param xRot  The initial pitch rotation in degrees.
     * @param owner The caster/owner entity, or {@code null}.
     */
    private void spawnSingleEntity(
            ServerLevel level,
            Vec3 pos,
            float yRot,
            float xRot,
            @Nullable LivingEntity owner
    ) {
        Entity spawned = this.entityType.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (spawned == null) {
            return;
        }

        spawned.snapTo(pos.x, pos.y, pos.z, yRot, xRot);

        if (spawned instanceof Mob mob) {
            mob.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(mob.blockPosition()),
                    EntitySpawnReason.MOB_SUMMONED,
                    null
            );
        }

        if (owner != null) {
            if (spawned instanceof Projectile projectile) {
                projectile.setOwner(owner);
            } else if (spawned instanceof EvokerFangs fangs) {
                fangs.setOwner(owner);
            }
        }

        level.addFreshEntity(spawned);
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link SummonEntityEffect} with uniform volume sampling.
     *
     * @param entityType The {@link EntityType} to summon.
     * @param count      The {@link LevelBasedValue} spawn count.
     */
    public SummonEntityEffect(EntityType<?> entityType, LevelBasedValue count) {
        this(entityType, count, Optional.empty(), Optional.empty());
    }

    /**
     * Constructs an unfiltered {@link SummonEntityEffect} with a custom spatial {@link ProbabilityDistribution}.
     *
     * @param entityType   The {@link EntityType} to summon.
     * @param count        The {@link LevelBasedValue} spawn count.
     * @param distribution The spatial probability distribution for volume sampling.
     */
    public SummonEntityEffect(EntityType<?> entityType, LevelBasedValue count, ProbabilityDistribution distribution) {
        this(entityType, count, Optional.of(distribution), Optional.empty());
    }

    /**
     * Constructs a {@link SummonEntityEffect} restricted by an entity predicate filter.
     *
     * @param entityType   The {@link EntityType} to summon.
     * @param count        The {@link LevelBasedValue} spawn count.
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public SummonEntityEffect(EntityType<?> entityType, LevelBasedValue count, ContextAwarePredicate entityFilter) {
        this(entityType, count, Optional.empty(), Optional.of(entityFilter));
    }

    /**
     * Constructs a {@link SummonEntityEffect} with both a custom {@link ProbabilityDistribution} and an entity filter.
     *
     * @param entityType   The {@link EntityType} to summon.
     * @param count        The {@link LevelBasedValue} spawn count.
     * @param distribution The spatial probability distribution for volume sampling.
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public SummonEntityEffect(EntityType<?> entityType, LevelBasedValue count, ProbabilityDistribution distribution, ContextAwarePredicate entityFilter) {
        this(entityType, count, Optional.of(distribution), Optional.of(entityFilter));
    }
    // endregion
}