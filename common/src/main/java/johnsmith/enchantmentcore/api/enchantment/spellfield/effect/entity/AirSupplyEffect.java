package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldEntityEffect} that replenishes (when positive) or depletes (when negative) a
 * target entity's oxygen air supply ticks, scaled by the entity's local topological multiplier.
 *
 * @param amount       The {@link LevelBasedValue} change in air supply ticks before topological scaling.
 * @param entityFilter Optional {@link ContextAwarePredicate} restricting which entities are affected.
 */
public record AirSupplyEffect(
        LevelBasedValue amount,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    /** JSON field key for the air supply tick delta. */
    public static final String AMOUNT = "amount";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "air_supply";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(AMOUNT);

    /**
     * MapCodec for serializing and deserializing {@link AirSupplyEffect} instances from JSON.
     */
    public static final MapCodec<AirSupplyEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(AMOUNT).forGetter(AirSupplyEffect::amount),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(AirSupplyEffect::entityFilter)
    ).apply(instance, AirSupplyEffect::new));

    /**
     * Modifies {@code victim}'s air supply by $\text{amount} \cdot \text{volumeScalar}$, clamped between
     * {@code -20} (drowning damage threshold) and {@link Entity#getMaxAirSupply()}.
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

        int change = (int) (this.amount.calculate(enchantmentLevel) * volumeScalar);
        if (change == 0) return;

        int newAir = Mth.clamp(victim.getAirSupply() + change, -20, victim.getMaxAirSupply());
        victim.setAirSupply(newAir);
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link AirSupplyEffect}.
     *
     * @param amount The {@link LevelBasedValue} air supply tick change.
     */
    public AirSupplyEffect(LevelBasedValue amount) {
        this(amount, Optional.empty());
    }

    /**
     * Constructs an {@link AirSupplyEffect} restricted by the given entity predicate filter.
     *
     * @param amount       The {@link LevelBasedValue} air supply tick change.
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public AirSupplyEffect(LevelBasedValue amount, ContextAwarePredicate entityFilter) {
        this(amount, Optional.of(entityFilter));
    }
    // endregion
}