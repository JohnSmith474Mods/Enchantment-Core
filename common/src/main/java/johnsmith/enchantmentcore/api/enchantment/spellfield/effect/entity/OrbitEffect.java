package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.DirectionalSpellFieldEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldAxis;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.function.ConstantScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.type.RotationDirection;
import johnsmith.enchantmentcore.api.registry.EnchantmentCoreRegistries;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A directional {@link SpellFieldEntityEffect} that guides targeted entities into a stable circular
 * or spherical orbit at a configurable {@link #orbitalRadius()} around the spell field epicenter.
 * <p>
 * When an orientation {@link #axis()} is provided, entities are pulled onto the orthogonal orbital plane
 * and accelerated tangentially around that axis; when {@link #axis()} is empty, entities enter a chaotic
 * 3D spherical shell orbit.
 *
 * @param attractionStrength The {@link LevelBasedValue} restoring force pulling entities toward the orbital ring/shell.
 * @param orbitalRadius      The {@link LevelBasedValue} target radius (in blocks) of the orbital ring from the epicenter.
 * @param orbitVelocity      The {@link LevelBasedValue} tangential speed imparted along the orbital path.
 * @param dragMultiplier     The {@link LevelBasedValue} horizontal velocity damping factor applied each tick prior to impulse addition.
 * @param distanceScaling    The {@link DistanceScalingFunction} modulating the radial restoring force based on distance from the target ring.
 * @param axis               Optional {@link FieldAxis} normal vector defining a structured 2D orbital plane.
 * @param rotationDirection  The {@link RotationDirection} (clockwise, counter-clockwise, or entity-ID parity) of the orbit.
 * @param entityFilter       Optional {@link ContextAwarePredicate} restricting which entities are pulled into orbit.
 */
public record OrbitEffect(
        LevelBasedValue attractionStrength,
        LevelBasedValue orbitalRadius,
        LevelBasedValue orbitVelocity,
        LevelBasedValue dragMultiplier,
        DistanceScalingFunction distanceScaling,
        Optional<FieldAxis> axis,
        RotationDirection rotationDirection,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect, DirectionalSpellFieldEffect {
    /** JSON field key for the radial restoring attraction strength. */
    public static final String ATTRACTION_STRENGTH = "attraction_strength";

    /** JSON field key for the target orbital radius. */
    public static final String ORBITAL_RADIUS = "orbital_radius";

    /** JSON field key for the tangential orbital speed. */
    public static final String ORBIT_VELOCITY = "orbit_velocity";

    /** JSON field key for the velocity damping drag factor. */
    public static final String DRAG_MULTIPLIER = "drag_multiplier";

    /** JSON field key for the radial error distance scaling function. */
    public static final String DISTANCE_SCALING = "distance_scaling";

    /** JSON field key for the optional orbital plane normal axis. */
    public static final String AXIS = "axis";

    /** JSON field key for the orbital rotation chirality. */
    public static final String ROTATION_DIRECTION = "rotation_direction";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "orbit";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            ATTRACTION_STRENGTH, ORBITAL_RADIUS, ORBIT_VELOCITY, DRAG_MULTIPLIER
    );

    /**
     * MapCodec for serializing and deserializing {@link OrbitEffect} instances from JSON.
     */
    public static final MapCodec<OrbitEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(ATTRACTION_STRENGTH).forGetter(OrbitEffect::attractionStrength),
            LevelBasedValue.CODEC.fieldOf(ORBITAL_RADIUS).forGetter(OrbitEffect::orbitalRadius),
            LevelBasedValue.CODEC.optionalFieldOf(ORBIT_VELOCITY, LevelBasedValue.constant(0.2F)).forGetter(OrbitEffect::orbitVelocity),
            LevelBasedValue.CODEC.optionalFieldOf(DRAG_MULTIPLIER, LevelBasedValue.constant(0.8F)).forGetter(OrbitEffect::dragMultiplier),
            EnchantmentCoreRegistries.getDistanceScalingFunctionCodec().optionalFieldOf(DISTANCE_SCALING, ConstantScalingFunction.NONE).forGetter(OrbitEffect::distanceScaling),
            FieldAxis.CODEC.optionalFieldOf(AXIS).forGetter(OrbitEffect::axis),
            RotationDirection.CODEC.optionalFieldOf(ROTATION_DIRECTION, RotationDirection.ANY).forGetter(OrbitEffect::rotationDirection),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(OrbitEffect::entityFilter)
    ).apply(instance, OrbitEffect::new));

    /**
     * Applies velocity damping, radial/planar ring-restoring attraction, and tangential orbital thrust
     * to {@code victim}, including anti-gravity compensation for floating items and experience orbs.
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

        Vec3 vectorToVictim = victim.position().subtract(epicenter);
        double distanceToCenter = vectorToVictim.length();
        if (distanceToCenter <= 0.0001D) return;

        float targetRadius = this.orbitalRadius.calculate(enchantmentLevel);

        double directionSign = switch (this.rotationDirection) {
            case CLOCKWISE -> -1.0D;
            case COUNTER_CLOCKWISE -> 1.0D;
            case ANY -> (victim.getId() % 2 == 0) ? 1.0D : -1.0D;
        };

        Vec3 correctionDir = Vec3.ZERO;
        Vec3 tangentialDir;
        double errorDistance;

        // CHAOTIC SPHERICAL ORBIT (Triggered if axis is empty)
        if (this.axis.isEmpty() || spatialReference == null) {
            double deltaToHalo = distanceToCenter - targetRadius;
            errorDistance = Math.abs(deltaToHalo);
            correctionDir = vectorToVictim.normalize().scale(-Math.signum(deltaToHalo));

            tangentialDir = new Vec3(vectorToVictim.z, 0, -vectorToVictim.x).normalize().scale(directionSign);
            if (tangentialDir.lengthSqr() < 0.0001D) {
                tangentialDir = new Vec3(1, 0, 0).scale(directionSign);
            }
        }
        // STRUCTURED PLANAR ORBIT
        else {
            Vec3 planeNormal = this.axis.get().getReferenceAxis(spatialReference);
            double distToPlane = vectorToVictim.dot(planeNormal);
            Vec3 projOnPlane = vectorToVictim.subtract(planeNormal.scale(distToPlane));
            double distToCenterOnPlane = projOnPlane.length();

            Vec3 planeCorrection = planeNormal.scale(-distToPlane);
            Vec3 ringCorrection = Vec3.ZERO;

            if (distToCenterOnPlane > 0.0001D) {
                double deltaToHalo = distToCenterOnPlane - targetRadius;
                ringCorrection = projOnPlane.normalize().scale(-deltaToHalo);
                tangentialDir = planeNormal.cross(projOnPlane).normalize().scale(directionSign);
            } else {
                tangentialDir = FieldAxis.LATERAL.getReferenceAxis(spatialReference).scale(directionSign);
            }

            Vec3 totalCorrection = planeCorrection.add(ringCorrection);
            errorDistance = totalCorrection.length();
            if (errorDistance > 0.0001D) {
                correctionDir = totalCorrection.normalize();
            }
        }

        // Apply physics payload
        float drag = this.dragMultiplier.calculate(enchantmentLevel);
        Vec3 currentDelta = victim.getDeltaMovement();
        Vec3 dampedDelta = new Vec3(currentDelta.x * drag, currentDelta.y * 0.4D, currentDelta.z * drag);

        Vec3 radialImpulse = Vec3.ZERO;
        if (correctionDir.lengthSqr() > 0.0001D) {
            float dampingScalar = this.distanceScaling.apply((float) errorDistance, targetRadius);
            float force = this.attractionStrength.calculate(enchantmentLevel) * dampingScalar * volumeScalar;
            radialImpulse = correctionDir.scale(force);
        }

        Vec3 orbitImpulse = Vec3.ZERO;
        if (tangentialDir.lengthSqr() > 0.0001D) {
            float orbitSpeed = this.orbitVelocity.calculate(enchantmentLevel) * volumeScalar;
            double orbitScalar = Math.max(0.0D, 1.0D - (errorDistance / (targetRadius * 2.0D)));
            orbitImpulse = tangentialDir.scale(orbitSpeed * orbitScalar);
        }

        double frictionBump = victim.onGround() ? 0.05D : 0.0D;
        double antiGravity = (victim instanceof net.minecraft.world.entity.item.ItemEntity || victim instanceof net.minecraft.world.entity.ExperienceOrb) ? 0.04D : 0.0D;

        victim.setDeltaMovement(dampedDelta.add(radialImpulse).add(orbitImpulse).add(0, frictionBump + antiGravity, 0));
        victim.hurtMarked = true;
        victim.needsSync = true;
    }

    @Override
    public Optional<FieldAxis> getActiveAxis() {
        return Optional.of(this.axis.orElse(FieldAxis.RADIAL));
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() { return CODEC; }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered spherical {@link OrbitEffect} (no fixed axis).
     *
     * @param attractionStrength The radial attraction strength.
     * @param orbitalRadius      The target orbital radius.
     * @param orbitVelocity      The tangential orbital velocity.
     * @param dragMultiplier     The velocity damping factor.
     * @param distanceScaling    The radial error distance scaling function.
     * @param rotationDirection  The orbital rotation direction.
     */
    public OrbitEffect(LevelBasedValue attractionStrength, LevelBasedValue orbitalRadius, LevelBasedValue orbitVelocity, LevelBasedValue dragMultiplier, DistanceScalingFunction distanceScaling, RotationDirection rotationDirection) {
        this(attractionStrength, orbitalRadius, orbitVelocity, dragMultiplier, distanceScaling, Optional.empty(), rotationDirection, Optional.empty());
    }

    /**
     * Constructs an unfiltered planar {@link OrbitEffect} around the specified {@link FieldAxis}.
     *
     * @param attractionStrength The radial attraction strength.
     * @param orbitalRadius      The target orbital radius.
     * @param orbitVelocity      The tangential orbital velocity.
     * @param dragMultiplier     The velocity damping factor.
     * @param distanceScaling    The radial error distance scaling function.
     * @param axis               The orbital plane normal axis.
     * @param rotationDirection  The orbital rotation direction.
     */
    public OrbitEffect(LevelBasedValue attractionStrength, LevelBasedValue orbitalRadius, LevelBasedValue orbitVelocity, LevelBasedValue dragMultiplier, DistanceScalingFunction distanceScaling, FieldAxis axis, RotationDirection rotationDirection) {
        this(attractionStrength, orbitalRadius, orbitVelocity, dragMultiplier, distanceScaling, Optional.of(axis), rotationDirection, Optional.empty());
    }

    /**
     * Constructs a spherical {@link OrbitEffect} restricted by an entity predicate filter.
     *
     * @param attractionStrength The radial attraction strength.
     * @param orbitalRadius      The target orbital radius.
     * @param orbitVelocity      The tangential orbital velocity.
     * @param dragMultiplier     The velocity damping factor.
     * @param distanceScaling    The radial error distance scaling function.
     * @param rotationDirection  The orbital rotation direction.
     * @param entityFilter       The predicate filter to match against candidate entities.
     */
    public OrbitEffect(LevelBasedValue attractionStrength, LevelBasedValue orbitalRadius, LevelBasedValue orbitVelocity, LevelBasedValue dragMultiplier, DistanceScalingFunction distanceScaling, RotationDirection rotationDirection, ContextAwarePredicate entityFilter) {
        this(attractionStrength, orbitalRadius, orbitVelocity, dragMultiplier, distanceScaling, Optional.empty(), rotationDirection, Optional.of(entityFilter));
    }

    /**
     * Constructs a planar {@link OrbitEffect} around the specified {@link FieldAxis} and restricted by an entity filter.
     *
     * @param attractionStrength The radial attraction strength.
     * @param orbitalRadius      The target orbital radius.
     * @param orbitVelocity      The tangential orbital velocity.
     * @param dragMultiplier     The velocity damping factor.
     * @param distanceScaling    The radial error distance scaling function.
     * @param axis               The orbital plane normal axis.
     * @param rotationDirection  The orbital rotation direction.
     * @param entityFilter       The predicate filter to match against candidate entities.
     */
    public OrbitEffect(LevelBasedValue attractionStrength, LevelBasedValue orbitalRadius, LevelBasedValue orbitVelocity, LevelBasedValue dragMultiplier, DistanceScalingFunction distanceScaling, FieldAxis axis, RotationDirection rotationDirection, ContextAwarePredicate entityFilter) {
        this(attractionStrength, orbitalRadius, orbitVelocity, dragMultiplier, distanceScaling, Optional.of(axis), rotationDirection, Optional.of(entityFilter));
    }
    // endregion
}