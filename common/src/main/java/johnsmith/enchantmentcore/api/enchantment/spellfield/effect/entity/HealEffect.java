package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldEntityEffect} that restores health points to targeted {@link LivingEntity}
 * instances within a spell field volume, scaled by each target's local topological multiplier.
 *
 * @param amount       The {@link LevelBasedValue} base health restored before topological scaling.
 * @param entityFilter Optional {@link ContextAwarePredicate} restricting which entities are healed.
 */
public record HealEffect(
        LevelBasedValue amount,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    /** JSON field key for the base healing amount. */
    public static final String AMOUNT = "amount";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "heal";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(AMOUNT);

    /**
     * MapCodec for serializing and deserializing {@link HealEffect} instances from JSON.
     */
    public static final MapCodec<HealEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(AMOUNT).forGetter(HealEffect::amount),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(HealEffect::entityFilter)
    ).apply(instance, HealEffect::new));

    /**
     * Restores $\text{amount} \cdot \text{volumeScalar}$ health to {@code victim} if it is a
     * {@link LivingEntity} and matches {@link #entityFilter()}.
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
        if (!(victim instanceof LivingEntity living)) return;
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        float finalHeal = this.amount.calculate(enchantmentLevel) * volumeScalar;
        if (finalHeal > 0.0F) {
            living.heal(finalHeal);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link HealEffect}.
     *
     * @param amount The {@link LevelBasedValue} base healing amount.
     */
    public HealEffect(LevelBasedValue amount) {
        this(amount, Optional.empty());
    }

    /**
     * Constructs a {@link HealEffect} restricted by the given entity predicate filter.
     *
     * @param amount       The {@link LevelBasedValue} base healing amount.
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public HealEffect(LevelBasedValue amount, ContextAwarePredicate entityFilter) {
        this(amount, Optional.of(entityFilter));
    }
    // endregion
}