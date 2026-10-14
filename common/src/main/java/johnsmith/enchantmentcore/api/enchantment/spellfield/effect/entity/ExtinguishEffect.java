package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldEntityEffect} that reduces the remaining burn duration ({@link Entity#getRemainingFireTicks()})
 * of burning entities within a spell field volume, scaled by each target's local topological multiplier.
 *
 * @param tickReduction The {@link LevelBasedValue} number of fire ticks subtracted before topological scaling.
 * @param entityFilter  Optional {@link ContextAwarePredicate} restricting which entities are extinguished.
 */
public record ExtinguishEffect(
        LevelBasedValue tickReduction,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    /** JSON field key for the fire tick reduction amount. */
    public static final String TICK_REDUCTION = "tick_reduction";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "extinguish";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(TICK_REDUCTION);

    /**
     * MapCodec for serializing and deserializing {@link ExtinguishEffect} instances from JSON.
     */
    public static final MapCodec<ExtinguishEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(TICK_REDUCTION).forGetter(ExtinguishEffect::tickReduction),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(ExtinguishEffect::entityFilter)
    ).apply(instance, ExtinguishEffect::new));

    /**
     * Subtracts $\text{tickReduction} \cdot \text{volumeScalar}$ from {@code victim}'s remaining fire ticks
     * (clamped to a minimum of {@code 0}) if the entity is currently burning.
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

        int reduction = (int) (this.tickReduction.calculate(enchantmentLevel) * volumeScalar);
        if (reduction <= 0) return;

        int currentFireTicks = victim.getRemainingFireTicks();
        if (currentFireTicks > 0) {
            victim.setRemainingFireTicks(Math.max(0, currentFireTicks - reduction));
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link ExtinguishEffect}.
     *
     * @param tickReduction The {@link LevelBasedValue} fire tick reduction amount.
     */
    public ExtinguishEffect(LevelBasedValue tickReduction) {
        this(tickReduction, Optional.empty());
    }

    /**
     * Constructs an {@link ExtinguishEffect} restricted by the given entity predicate filter.
     *
     * @param tickReduction The {@link LevelBasedValue} fire tick reduction amount.
     * @param entityFilter  The predicate filter to match against candidate entities.
     */
    public ExtinguishEffect(LevelBasedValue tickReduction, ContextAwarePredicate entityFilter) {
        this(tickReduction, Optional.of(entityFilter));
    }
    // endregion
}