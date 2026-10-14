package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldEntityEffect} that reduces the remaining duration of all active status effects
 * on targeted {@link LivingEntity} instances by an amount scaled by the entity's local topological
 * multiplier, removing any effect whose remaining duration drops to or below zero.
 *
 * @param durationReduction The {@link LevelBasedValue} number of ticks subtracted from each active status effect before topological scaling.
 * @param entityFilter      Optional {@link ContextAwarePredicate} restricting which entities are affected.
 */
public record ClearEffectsEffect(
        LevelBasedValue durationReduction,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    /** JSON field key for the status effect duration reduction in ticks. */
    public static final String DURATION_REDUCTION = "duration_reduction";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "clear_effects";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(DURATION_REDUCTION);

    /**
     * MapCodec for serializing and deserializing {@link ClearEffectsEffect} instances from JSON.
     */
    public static final MapCodec<ClearEffectsEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(DURATION_REDUCTION).forGetter(ClearEffectsEffect::durationReduction),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(ClearEffectsEffect::entityFilter)
    ).apply(instance, ClearEffectsEffect::new));

    /**
     * Reduces the duration of all active {@link MobEffectInstance}s on {@code victim} by
     * $\text{durationReduction} \cdot \text{volumeScalar}$, removing expired effects and updating surviving ones.
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

        int reduction = (int) (this.durationReduction.calculate(enchantmentLevel) * volumeScalar);
        if (reduction <= 0) return;

        List<MobEffectInstance> toReplace = new ArrayList<>();
        List<Holder<MobEffect>> toRemove = new ArrayList<>();

        for (MobEffectInstance instance : living.getActiveEffects()) {
            int newDuration = instance.getDuration() - reduction;
            if (newDuration <= 0) {
                toRemove.add(instance.getEffect());
            } else {
                MobEffectInstance modifiedInstance = new MobEffectInstance(
                        instance.getEffect(),
                        newDuration,
                        instance.getAmplifier(),
                        instance.isAmbient(),
                        instance.isVisible(),
                        instance.showIcon()
                );
                toReplace.add(modifiedInstance);
            }
        }

        for (Holder<MobEffect> effect : toRemove) {
            living.removeEffect(effect);
        }

        for (MobEffectInstance modifiedInstance : toReplace) {
            living.addEffect(modifiedInstance);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link ClearEffectsEffect}.
     *
     * @param durationReduction The {@link LevelBasedValue} duration reduction in ticks.
     */
    public ClearEffectsEffect(LevelBasedValue durationReduction) {
        this(durationReduction, Optional.empty());
    }

    /**
     * Constructs a {@link ClearEffectsEffect} restricted by the given entity predicate filter.
     *
     * @param durationReduction The {@link LevelBasedValue} duration reduction in ticks.
     * @param entityFilter      The predicate filter to match against candidate entities.
     */
    public ClearEffectsEffect(LevelBasedValue durationReduction, ContextAwarePredicate entityFilter) {
        this(durationReduction, Optional.of(entityFilter));
    }
    // endregion
}