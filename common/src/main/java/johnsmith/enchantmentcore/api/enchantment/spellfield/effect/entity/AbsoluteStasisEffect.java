package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldEntityEffect} that zeroes out a target entity's velocity vector and resets its
 * accumulated {@link Entity#fallDistance}, freezing the entity in mid-air without lethal fall damage buildup.
 *
 * @param entityFilter Optional {@link ContextAwarePredicate} restricting which entities are affected.
 */
public record AbsoluteStasisEffect(
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "absolute_stasis";

    /**
     * MapCodec for serializing and deserializing {@link AbsoluteStasisEffect} instances from JSON.
     */
    public static final MapCodec<AbsoluteStasisEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ContextAwarePredicate.CODEC.optionalFieldOf("entity_filter").forGetter(AbsoluteStasisEffect::entityFilter)
    ).apply(instance, AbsoluteStasisEffect::new));

    /**
     * Clamps {@code victim}'s momentum to {@link Vec3#ZERO}, resets {@code fallDistance} to {@code 0.0F},
     * and marks the entity for immediate client velocity synchronization.
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

        // 1. Clamp all momentum and gravity accumulation
        victim.setDeltaMovement(Vec3.ZERO);

        // 2. Prevent lethal fall damage from accumulating while suspended
        victim.fallDistance = 0.0F;

        // 3. Force the server to synchronize the frozen state to the client
        victim.hasImpulse = true;
        victim.hurtMarked = true;
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link AbsoluteStasisEffect} affecting all targeted entities.
     */
    public AbsoluteStasisEffect() {
        this(Optional.empty());
    }

    /**
     * Constructs an {@link AbsoluteStasisEffect} restricted by the given entity predicate filter.
     *
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public AbsoluteStasisEffect(ContextAwarePredicate entityFilter) {
        this(Optional.of(entityFilter));
    }
    // endregion
}