package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.mixin.enchantment.accessor.ExperienceOrbAccessor;
import johnsmith.enchantmentcore.mixin.enchantment.accessor.ItemEntityAccessor;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldEntityEffect} that resets the internal despawn age timer of targeted
 * {@link ItemEntity} and {@link ExperienceOrb} instances back to {@code 0} ticks, preventing
 * them from despawning while inside the spell field.
 *
 * @param entityFilter Optional {@link ContextAwarePredicate} restricting which entities have their age reset.
 */
public record ResetLifetimeEffect(
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "reset_lifetime";

    /**
     * MapCodec for serializing and deserializing {@link ResetLifetimeEffect} instances from JSON.
     */
    public static final MapCodec<ResetLifetimeEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ContextAwarePredicate.CODEC.optionalFieldOf("entity_filter").forGetter(ResetLifetimeEffect::entityFilter)
    ).apply(instance, ResetLifetimeEffect::new));

    /**
     * Resets the internal {@code age} field to {@code 0} if {@code victim} is an {@link ItemEntity}
     * or {@link ExperienceOrb} and matches {@link #entityFilter()}.
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

        // Reset the despawn timer to 0 for floating entities
        if (victim instanceof ItemEntity item) {
            ((ItemEntityAccessor) item).enchantment_core$setAge(0);
        } else if (victim instanceof ExperienceOrb orb) {
            ((ExperienceOrbAccessor) orb).enchantment_core$setAge(0);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link ResetLifetimeEffect}.
     */
    public ResetLifetimeEffect() {
        this(Optional.empty());
    }

    /**
     * Constructs a {@link ResetLifetimeEffect} restricted by the given entity predicate filter.
     *
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public ResetLifetimeEffect(ContextAwarePredicate entityFilter) {
        this(Optional.of(entityFilter));
    }
    // endregion
}