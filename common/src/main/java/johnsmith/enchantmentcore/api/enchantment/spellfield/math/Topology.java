package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import java.util.Optional;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Contract specifying the mathematical rule set that governs internal volume strength falloff,
 * directional axis projection, and bounding box generation for a spell field volume.
 */
public interface Topology {
    /**
     * Retrieves the primary operational range radiating from the spatial epicenter.
     *
     * @return The {@link LevelBasedValue} governing the maximum origin distance.
     */
    LevelBasedValue originRange();

    /**
     * Retrieves the optional distance limit extending parallel to the primary orientation {@link #axis()}.
     *
     * @return An {@link Optional} containing the axial range {@link LevelBasedValue}.
     */
    Optional<LevelBasedValue> axialRange();

    /**
     * Retrieves the optional distance limit extending perpendicular to the primary orientation {@link #axis()}.
     *
     * @return An {@link Optional} containing the radial range {@link LevelBasedValue}.
     */
    Optional<LevelBasedValue> radialRange();

    /**
     * Retrieves the optional spatial displacement separating the topological origin from the volume center.
     *
     * @return An {@link Optional} containing the origin offset {@link SpatialVector}.
     */
    Optional<SpatialVector> originOffset();

    /**
     * Retrieves the optional directional axis vector governing cylindrical/planar field alignment.
     *
     * @return An {@link Optional} containing the orientation axis {@link SpatialVector}.
     */
    Optional<SpatialVector> axis();

    /**
     * Retrieves the falloff scaling function applied to a target's distance from the topological origin.
     *
     * @return The origin {@link DistanceScalingFunction}.
     */
    DistanceScalingFunction originScaling();

    /**
     * Retrieves the falloff scaling function applied to a target's distance parallel to {@link #axis()}.
     *
     * @return The axial {@link DistanceScalingFunction}.
     */
    DistanceScalingFunction axialScaling();

    /**
     * Retrieves the falloff scaling function applied to a target's distance perpendicular to {@link #axis()}.
     *
     * @return The radial {@link DistanceScalingFunction}.
     */
    DistanceScalingFunction radialScaling();

    /**
     * Retrieves the distance metric ($L_2$ Euclidean or $L_1$ Manhattan) used for radius comparisons.
     *
     * @return The active {@link DistanceMetric}.
     */
    DistanceMetric metric();

    /**
     * Calculates the absolute world-space origin coordinate for topological evaluation by applying
     * {@link #originOffset()} to the given {@code epicenter}.
     *
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param spatialReference The entity defining the local reference frame, or {@code null}.
     * @param epicenter        The base mathematical center of the volume.
     * @return The offset world-space origin coordinate.
     */
    Vec3 getOrigin(int enchantmentLevel, Entity spatialReference, Vec3 epicenter);

    /**
     * Constructs a tight world-space axis-aligned bounding box enclosing the active topological range.
     *
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param spatialReference The entity defining the local reference frame, or {@code null}.
     * @param volumeCenter     The mathematical center of the volume.
     * @return The computed world-space {@link AABB}.
     */
    AABB computeAABB(int enchantmentLevel, Entity spatialReference, Vec3 volumeCenter);

    /**
     * Evaluates the composite topological multiplier ($\text{origin} \cdot \text{axial} \cdot \text{radial}$)
     * for a target at {@code targetPos}.
     *
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param spatialReference The entity defining the local reference frame, or {@code null}.
     * @param epicenter        The mathematical center of the field volume.
     * @param targetPos        The absolute world position of the target entity or block.
     * @return The scalar multiplier in $[0.0, 1.0]$, or {@code 0.0F} if the target lies outside any active range bound.
     */
    float evaluateMultiplier(int enchantmentLevel, Entity spatialReference, Vec3 epicenter, Vec3 targetPos);
}