package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.DirectionalSpellFieldEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldAxis;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A directional {@link SpellFieldEntityEffect} that applies a kinetic velocity impulse to targeted
 * entities along a configured {@link FieldAxis} (defaulting to {@link FieldAxis#RADIAL}), scaled by
 * each entity's local topological multiplier.
 * <p>
 * Implements {@link DirectionalSpellFieldEffect} to expose its active axis to visual particle fields
 * and the client-side spell field debug renderer.
 *
 * @param strength     The {@link LevelBasedValue} peak impulse magnitude (positive pushes along the axis; negative pulls against it).
 * @param direction    Optional {@link FieldAxis} dictating the impulse vector geometry (defaults to {@link FieldAxis#RADIAL}).
 * @param entityFilter Optional {@link ContextAwarePredicate} restricting which entities receive the impulse.
 */
public record ImpulseEffect(
        LevelBasedValue strength,
        Optional<FieldAxis> direction,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect, DirectionalSpellFieldEffect {
    /** JSON field key for the impulse force magnitude. */
    public static final String STRENGTH = "strength";

    /** JSON field key for the directional field axis. */
    public static final String DIRECTION = "direction";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "impulse";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(STRENGTH);

    /**
     * MapCodec for serializing and deserializing {@link ImpulseEffect} instances from JSON.
     */
    public static final MapCodec<ImpulseEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(STRENGTH).forGetter(ImpulseEffect::strength),
            FieldAxis.CODEC.optionalFieldOf(DIRECTION).forGetter(ImpulseEffect::direction),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(ImpulseEffect::entityFilter)
    ).apply(instance, ImpulseEffect::new));

    /**
     * Computes the directional unit vector from {@link #direction()} (or {@link FieldAxis#RADIAL}),
     * scales it by $\text{strength} \cdot \text{volumeScalar}$, adds a small vertical friction-overcoming
     * bump for grounded horizontal impulses, and updates {@code victim}'s velocity.
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

        float force = this.strength.calculate(enchantmentLevel) * volumeScalar;
        if (Math.abs(force) < 0.0001F) return;

        FieldAxis activeDirection = this.direction.orElse(FieldAxis.RADIAL);
        Vec3 impulseDir = activeDirection.getImpulseVector(spatialReference, epicenter, victim.position());
        Vec3 impulse = impulseDir.scale(force);

        double frictionBump = (victim.onGround() && activeDirection != FieldAxis.VERTICAL && activeDirection != FieldAxis.RADIAL)
                ? Math.min(0.2D, Math.abs(force))
                : 0.0D;

        victim.setDeltaMovement(victim.getDeltaMovement().add(impulse.x, impulse.y + frictionBump, impulse.z));
        victim.hurtMarked = true;
        victim.needsSync = true;
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() { return CODEC; }

    @Override
    public Optional<FieldAxis> getActiveAxis() {
        return Optional.of(this.direction.orElse(FieldAxis.RADIAL));
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered radial {@link ImpulseEffect}.
     *
     * @param strength The {@link LevelBasedValue} impulse strength.
     */
    public ImpulseEffect(LevelBasedValue strength) {
        this(strength, Optional.empty(), Optional.empty());
    }

    /**
     * Constructs an unfiltered {@link ImpulseEffect} along the specified {@link FieldAxis}.
     *
     * @param strength  The {@link LevelBasedValue} impulse strength.
     * @param direction The {@link FieldAxis} direction.
     */
    public ImpulseEffect(LevelBasedValue strength, FieldAxis direction) {
        this(strength, Optional.of(direction), Optional.empty());
    }

    /**
     * Constructs a radial {@link ImpulseEffect} restricted by the given entity predicate filter.
     *
     * @param strength     The {@link LevelBasedValue} impulse strength.
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public ImpulseEffect(LevelBasedValue strength, ContextAwarePredicate entityFilter) {
        this(strength, Optional.empty(), Optional.of(entityFilter));
    }

    /**
     * Constructs an {@link ImpulseEffect} along the specified {@link FieldAxis} and restricted by an entity filter.
     *
     * @param strength     The {@link LevelBasedValue} impulse strength.
     * @param direction    The {@link FieldAxis} direction.
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public ImpulseEffect(LevelBasedValue strength, FieldAxis direction, ContextAwarePredicate entityFilter) {
        this(strength, Optional.of(direction), Optional.of(entityFilter));
    }
    // endregion
}