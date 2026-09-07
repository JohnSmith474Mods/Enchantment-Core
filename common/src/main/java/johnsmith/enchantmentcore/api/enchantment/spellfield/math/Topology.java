package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import java.util.Optional;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Specifies the mathematical rule set defining internal volume strength falloff and bounds generation.
 */
public interface Topology {
    /**
     * @return The primary operational limit radiating from the spatial epicenter.
     */
    LevelBasedValue originRange();

    /**
     * @return The specific operational limit dictating the radius extending along the primary orientation axis.
     */
    Optional<LevelBasedValue> axialRange();

    /**
     * @return The specific operational limit dictating the radius extending perpendicular to the primary orientation axis.
     */
    Optional<LevelBasedValue> radialRange();

    /**
     * @return The localized physical displacement separating the epicenter from the target focal point.
     */
    Optional<SpatialVector> originOffset();

    /**
     * @return The directional line dominating field alignment and spatial distortion functions.
     */
    Optional<SpatialVector> axis();

    /**
     * @return The falloff mapping algorithm applied to targets moving away from the operational epicenter.
     */
    DistanceScalingFunction originScaling();

    /**
     * @return The falloff mapping algorithm applied to targets moving along the orientation axis.
     */
    DistanceScalingFunction axialScaling();

    /**
     * @return The falloff mapping algorithm applied to targets moving perpendicular to the orientation axis.
     */
    DistanceScalingFunction radialScaling();

    /**
     * @return The specific distance measuring logic (e.g., Euclidean vs Manhattan) for radius comparisons.
     */
    DistanceMetric metric();

    /**
     * Calculates the exact absolute starting origin coordinate for topological mapping.
     *
     * @param enchantmentLevel The level of the spell field enchantment.
     * @param spatialReference The entity defining the reference frame.
     * @param epicenter        The base mathematical center of the volume.
     * @return The offset absolute origin coordinate.
     */
    Vec3 getOrigin(int enchantmentLevel, Entity spatialReference, Vec3 epicenter);

    /**
     * Constructs the minimal bounding box enclosing the calculated geometry.
     *
     * @param enchantmentLevel The level of the spell field enchantment.
     * @param spatialReference The entity defining the reference frame.
     * @param volumeCenter     The mathematical center of the volume.
     * @return The strict geometric bounds.
     */
    AABB computeAABB(int enchantmentLevel, Entity spatialReference, Vec3 volumeCenter);

    /**
     * Calculates the effective multiplier applied to a target at a designated spatial coordinate based on falloff scaling.
     *
     * @param enchantmentLevel The level of the spell field enchantment.
     * @param spatialReference The entity defining the reference frame.
     * @param epicenter        The mathematical center of the field volume.
     * @param targetPos        The absolute world position of the specific entity or block target.
     * @return The scalar multiplier bounded between 0.0 and 1.0. Returns 0.0 if the target is outside maximum range limits.
     */
    float evaluateMultiplier(int enchantmentLevel, Entity spatialReference, Vec3 epicenter, Vec3 targetPos);
}