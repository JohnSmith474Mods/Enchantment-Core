package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.volume;

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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Dual-purpose spell field effect implementing both {@link SpellFieldEntityEffect} and
 * {@link SpellFieldVolumeEffect} that scans vertically downward from a target or sampled coordinate
 * to locate the nearest solid ground surface and spawns a hazard entity (such as {@link EvokerFangs})
 * directly atop that block.
 *
 * @param entityType     The hazard {@link EntityType} to instantiate and place on the surface.
 * @param count          The {@link LevelBasedValue} number of surface hazards spawned when evaluated as a volume effect.
 * @param verticalSearch The {@link LevelBasedValue} vertical half-range (in blocks above and below the target point) searched for a solid surface.
 * @param distribution   Optional {@link ProbabilityDistribution} used to sample horizontal/vertical starting coordinates when evaluated as a volume effect.
 * @param entityFilter   Optional {@link ContextAwarePredicate} restricting which entities trigger hazards when evaluated as an entity effect.
 */
public record SurfaceHazardEffect(
        EntityType<?> entityType,
        LevelBasedValue count,
        LevelBasedValue verticalSearch,
        Optional<ProbabilityDistribution> distribution,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect, SpellFieldVolumeEffect {
    /** JSON field key for the hazard entity type identifier. */
    public static final String ENTITY_TYPE = "entity_type";

    /** JSON field key for the volume hazard count. */
    public static final String COUNT = "count";

    /** JSON field key for the vertical ground search range in blocks. */
    public static final String VERTICAL_SEARCH = "vertical_search";

    /** JSON field key for the optional spatial probability distribution. */
    public static final String DISTRIBUTION = "distribution";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field effect. */
    public static final String KEY = "surface_hazard";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(COUNT, VERTICAL_SEARCH);

    /**
     * MapCodec for serializing and deserializing {@link SurfaceHazardEffect} instances from JSON.
     */
    public static final MapCodec<SurfaceHazardEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf(ENTITY_TYPE).forGetter(SurfaceHazardEffect::entityType),
            LevelBasedValue.CODEC.optionalFieldOf(COUNT, LevelBasedValue.constant(1.0F)).forGetter(SurfaceHazardEffect::count),
            LevelBasedValue.CODEC.optionalFieldOf(VERTICAL_SEARCH, LevelBasedValue.constant(5.0F)).forGetter(SurfaceHazardEffect::verticalSearch),
            ProbabilityDistribution.CODEC.optionalFieldOf(DISTRIBUTION).forGetter(SurfaceHazardEffect::distribution),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(SurfaceHazardEffect::entityFilter)
    ).apply(instance, SurfaceHazardEffect::new));

    /**
     * Searches vertically around {@code victim}'s position and spawns a surface hazard beneath the
     * entity if it matches {@link #entityFilter()}.
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
        this.dispatchHazard(level, enchantmentLevel, context.owner(), victim.position(), volumeScalar);
    }

    /**
     * Samples {@link #count()} points across {@code volumes} using {@link #distribution()} (or uniform
     * sampling if absent) and spawns a surface hazard on the ground below each sampled point.
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

        int totalHazards = (int) this.count.calculate(enchantmentLevel);

        for (int i = 0; i < totalHazards; i++) {
            Vec3 targetPoint = this.distribution
                    .map(d -> d.samplePoint(level, enchantmentLevel, spatialReference, epicenter, volumes))
                    .orElseGet(() -> ProbabilityDistribution.getUniformPoint(volumes, level.random));
            this.dispatchHazard(level, enchantmentLevel, context.owner(), targetPoint, 1.0F);
        }
    }

    /**
     * Scans downward from $\text{targetPos.y} + \text{verticalSearch}$ to $\text{targetPos.y} - \text{verticalSearch}$
     * for the first block with a non-empty collision shape, spawning {@link #entityType()} at
     * $(\text{x} + 0.5, \text{y} + 1.0, \text{z} + 0.5)$ and assigning {@code owner} if applicable.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param owner            The caster/owner entity, or {@code null}.
     * @param targetPos        The starting world-space coordinate for the vertical search.
     * @param scalar           The topological multiplier scalar.
     */
    private void dispatchHazard(ServerLevel level, int enchantmentLevel, LivingEntity owner, Vec3 targetPos, float scalar) {
        if (scalar <= 0.0001F) return;

        int range = (int) this.verticalSearch.calculate(enchantmentLevel);
        BlockPos.MutableBlockPos cursor = BlockPos.containing(targetPos).mutable();
        cursor.setY(cursor.getY() + range);

        boolean found = false;
        for (int y = 0; y <= range * 2; y++) {
            BlockState state = level.getBlockState(cursor);
            VoxelShape collision = state.getCollisionShape(level, cursor);
            if (!collision.isEmpty()) {
                found = true;
                break;
            }
            cursor.move(0, -1, 0);
        }

        if (found) {
            Entity hazard = this.entityType.create(level, EntitySpawnReason.TRIGGERED);
            if (hazard != null) {
                hazard.setPos(cursor.getX() + 0.5D, cursor.getY() + 1.0D, cursor.getZ() + 0.5D);
                if (owner != null) {
                    if (hazard instanceof Projectile projectile) {
                        projectile.setOwner(owner);
                    } else if (hazard instanceof EvokerFangs fangs) {
                        fangs.setOwner(owner);
                    }
                }
                level.addFreshEntity(hazard);
            }
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link SurfaceHazardEffect} with uniform volume sampling.
     *
     * @param entityType     The hazard {@link EntityType} to spawn.
     * @param count          The number of volume hazards to spawn.
     * @param verticalSearch The vertical search radius in blocks.
     */
    public SurfaceHazardEffect(EntityType<?> entityType, LevelBasedValue count, LevelBasedValue verticalSearch) {
        this(entityType, count, verticalSearch, Optional.empty(), Optional.empty());
    }

    /**
     * Constructs an unfiltered {@link SurfaceHazardEffect} with a custom spatial {@link ProbabilityDistribution}.
     *
     * @param entityType     The hazard {@link EntityType} to spawn.
     * @param count          The number of volume hazards to spawn.
     * @param verticalSearch The vertical search radius in blocks.
     * @param distribution   The spatial probability distribution for volume sampling.
     */
    public SurfaceHazardEffect(EntityType<?> entityType, LevelBasedValue count, LevelBasedValue verticalSearch, ProbabilityDistribution distribution) {
        this(entityType, count, verticalSearch, Optional.of(distribution), Optional.empty());
    }

    /**
     * Constructs a {@link SurfaceHazardEffect} restricted by an entity predicate filter.
     *
     * @param entityType     The hazard {@link EntityType} to spawn.
     * @param count          The number of volume hazards to spawn.
     * @param verticalSearch The vertical search radius in blocks.
     * @param entityFilter   The predicate filter to match against candidate entities.
     */
    public SurfaceHazardEffect(EntityType<?> entityType, LevelBasedValue count, LevelBasedValue verticalSearch, ContextAwarePredicate entityFilter) {
        this(entityType, count, verticalSearch, Optional.empty(), Optional.of(entityFilter));
    }

    /**
     * Constructs a {@link SurfaceHazardEffect} with both a custom {@link ProbabilityDistribution} and an entity filter.
     *
     * @param entityType     The hazard {@link EntityType} to spawn.
     * @param count          The number of volume hazards to spawn.
     * @param verticalSearch The vertical search radius in blocks.
     * @param distribution   The spatial probability distribution for volume sampling.
     * @param entityFilter   The predicate filter to match against candidate entities.
     */
    public SurfaceHazardEffect(EntityType<?> entityType, LevelBasedValue count, LevelBasedValue verticalSearch, ProbabilityDistribution distribution, ContextAwarePredicate entityFilter) {
        this(entityType, count, verticalSearch, Optional.of(distribution), Optional.of(entityFilter));
    }
    // endregion
}