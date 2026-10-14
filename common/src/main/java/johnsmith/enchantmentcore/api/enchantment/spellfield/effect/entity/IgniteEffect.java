package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldEntityEffect} that sets targeted entities on fire for a configured duration
 * in seconds, scaled by each target's local topological multiplier.
 *
 * @param durationSeconds The {@link LevelBasedValue} base burn duration in seconds before topological scaling.
 * @param entityFilter    Optional {@link ContextAwarePredicate} restricting which entities are ignited.
 */
public record IgniteEffect(
        LevelBasedValue durationSeconds,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    /** JSON field key for the burn duration in seconds. */
    public static final String DURATION_SECONDS = "duration_seconds";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "ignite";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(DURATION_SECONDS);

    /**
     * MapCodec for serializing and deserializing {@link IgniteEffect} instances from JSON.
     */
    public static final MapCodec<IgniteEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(DURATION_SECONDS).forGetter(IgniteEffect::durationSeconds),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(IgniteEffect::entityFilter)
    ).apply(instance, IgniteEffect::new));

    /**
     * Ignites {@code victim} for $\text{durationSeconds} \cdot \text{volumeScalar}$ seconds via
     * {@link Entity#igniteForSeconds(float)} if it matches {@link #entityFilter()}.
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

        float finalDuration = this.durationSeconds.calculate(enchantmentLevel) * volumeScalar;
        if (finalDuration > 0.0F) {
            victim.igniteForSeconds(finalDuration);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link IgniteEffect}.
     *
     * @param durationSeconds The {@link LevelBasedValue} base burn duration in seconds.
     */
    public IgniteEffect(LevelBasedValue durationSeconds) {
        this(durationSeconds, Optional.empty());
    }

    /**
     * Constructs an {@link IgniteEffect} restricted by the given entity predicate filter.
     *
     * @param durationSeconds The {@link LevelBasedValue} base burn duration in seconds.
     * @param entityFilter    The predicate filter to match against candidate entities.
     */
    public IgniteEffect(LevelBasedValue durationSeconds, ContextAwarePredicate entityFilter) {
        this(durationSeconds, Optional.of(entityFilter));
    }
    // endregion
}