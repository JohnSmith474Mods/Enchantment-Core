package johnsmith.enchantmentcore.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.DirectionalSpellFieldEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.math.function.ConstantScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldAxis;
import johnsmith.enchantmentcore.enchantment.spellfield.type.RotationDirection;
import johnsmith.enchantmentcore.registry.EnchantmentCoreRegistries;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

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

    public static final String ATTRACTION_STRENGTH = "attraction_strength";
    public static final String ORBITAL_RADIUS = "orbital_radius";
    public static final String ORBIT_VELOCITY = "orbit_velocity";
    public static final String DRAG_MULTIPLIER = "drag_multiplier";
    public static final String DISTANCE_SCALING = "distance_scaling";
    public static final String AXIS = "axis";
    public static final String ROTATION_DIRECTION = "rotation_direction";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "orbit";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            ATTRACTION_STRENGTH, ORBITAL_RADIUS, ORBIT_VELOCITY, DRAG_MULTIPLIER
    );

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
        victim.hasImpulse = true;
    }

    @Override
    public Optional<FieldAxis> getActiveAxis() {
        return Optional.of(this.axis.orElse(FieldAxis.RADIAL));
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() { return CODEC; }
}