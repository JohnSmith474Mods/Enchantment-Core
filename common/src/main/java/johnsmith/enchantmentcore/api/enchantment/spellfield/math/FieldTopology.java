package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.function.ConstantScalingFunction;
import johnsmith.enchantmentcore.api.registry.EnchantmentCoreRegistries;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Standard data-driven record implementation of {@link Topology} defining radial, axial, and
 * omnidirectional distance falloff rules and tight bounding-box computation for a {@link johnsmith.enchantmentcore.api.enchantment.spellfield.shape.SpellFieldVolume}.
 *
 * @param originRange   The primary operational radius radiating from the topological origin.
 * @param axialRange    Optional distance limit extending parallel to {@link #axis()}.
 * @param radialRange   Optional distance limit extending perpendicular to {@link #axis()}.
 * @param originOffset  Optional local/global {@link SpatialVector} offset shifting the topological origin from the volume center.
 * @param axis          Optional {@link SpatialVector} defining the cylindrical orientation axis for axial/radial decomposition.
 * @param originScaling The {@link DistanceScalingFunction} applied to the overall distance from the origin.
 * @param axialScaling  The {@link DistanceScalingFunction} applied to the distance component parallel to {@link #axis()}.
 * @param radialScaling The {@link DistanceScalingFunction} applied to the distance component perpendicular to {@link #axis()}.
 * @param metric        The {@link DistanceMetric} ($L_2$ Euclidean or $L_1$ Manhattan) used for distance evaluation.
 */
public record FieldTopology(
        LevelBasedValue originRange,
        Optional<LevelBasedValue> axialRange,
        Optional<LevelBasedValue> radialRange,
        Optional<SpatialVector> originOffset,
        Optional<SpatialVector> axis,
        DistanceScalingFunction originScaling,
        DistanceScalingFunction axialScaling,
        DistanceScalingFunction radialScaling,
        DistanceMetric metric
) implements Topology {
    /** JSON field key for the primary origin range. */
    public static final String ORIGIN_RANGE = "origin_range";

    /** JSON field key for the optional axial range along the orientation axis. */
    public static final String AXIAL_RANGE = "axial_range";

    /** JSON field key for the optional radial range perpendicular to the orientation axis. */
    public static final String RADIAL_RANGE = "radial_range";

    /** JSON field key for the optional topological origin offset vector. */
    public static final String ORIGIN_OFFSET = "origin_offset";

    /** JSON field key for the optional orientation axis vector. */
    public static final String AXIS = "axis";

    /** JSON field key for the origin distance scaling function. */
    public static final String ORIGIN_SCALING = "origin_scaling";

    /** JSON field key for the axial distance scaling function. */
    public static final String AXIAL_SCALING = "axial_scaling";

    /** JSON field key for the radial distance scaling function. */
    public static final String RADIAL_SCALING = "radial_scaling";

    /** JSON field key for the spatial distance metric. */
    public static final String METRIC = "metric";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> {
        Set<String> set = new HashSet<>(Set.of(ORIGIN_RANGE, AXIAL_RANGE, RADIAL_RANGE));
        set.addAll(SpatialVector.KEY_PROVIDER.getLevelBasedKeys());
        return set;
    };

    /**
     * Codec for serializing and deserializing {@link FieldTopology} instances from JSON.
     */
    public static final Codec<FieldTopology> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(ORIGIN_RANGE).forGetter(FieldTopology::originRange),
            LevelBasedValue.CODEC.optionalFieldOf(AXIAL_RANGE).forGetter(FieldTopology::axialRange),
            LevelBasedValue.CODEC.optionalFieldOf(RADIAL_RANGE).forGetter(FieldTopology::radialRange),
            SpatialVector.CODEC.optionalFieldOf(ORIGIN_OFFSET).forGetter(FieldTopology::originOffset),
            SpatialVector.CODEC.optionalFieldOf(AXIS).forGetter(FieldTopology::axis),
            EnchantmentCoreRegistries.getDistanceScalingFunctionCodec().optionalFieldOf(ORIGIN_SCALING, ConstantScalingFunction.NONE).forGetter(FieldTopology::originScaling),
            EnchantmentCoreRegistries.getDistanceScalingFunctionCodec().optionalFieldOf(AXIAL_SCALING, ConstantScalingFunction.NONE).forGetter(FieldTopology::axialScaling),
            EnchantmentCoreRegistries.getDistanceScalingFunctionCodec().optionalFieldOf(RADIAL_SCALING, ConstantScalingFunction.NONE).forGetter(FieldTopology::radialScaling),
            DistanceMetric.CODEC.optionalFieldOf(METRIC, DistanceMetric.EUCLIDEAN).forGetter(FieldTopology::metric)
    ).apply(instance, FieldTopology::new));

    @Override
    public Vec3 getOrigin(int enchantmentLevel, Entity spatialReference, Vec3 epicenter) {
        return epicenter.add(this.originOffset.orElse(SpatialVector.ZERO).resolve(enchantmentLevel, spatialReference));
    }

    @Override
    public AABB computeAABB(int enchantmentLevel, Entity spatialReference, Vec3 volumeCenter) {
        Vec3 origin = this.getOrigin(enchantmentLevel, spatialReference, volumeCenter);
        float oRange = this.originRange.calculate(enchantmentLevel);

        if (this.axis.isPresent() && this.axialRange.isPresent() && this.radialRange.isPresent()) {
            Vec3 dir = this.axis.get().resolve(enchantmentLevel, spatialReference);
            if (dir.lengthSqr() > 0.0001D) {
                dir = dir.normalize();
                float h = this.axialRange.get().calculate(enchantmentLevel);
                float r = this.radialRange.get().calculate(enchantmentLevel);

                double ex = h * Math.abs(dir.x) + r * Math.sqrt(Math.max(0.0, 1.0 - dir.x * dir.x));
                double ey = h * Math.abs(dir.y) + r * Math.sqrt(Math.max(0.0, 1.0 - dir.y * dir.y));
                double ez = h * Math.abs(dir.z) + r * Math.sqrt(Math.max(0.0, 1.0 - dir.z * dir.z));

                ex = Math.min(ex, oRange);
                ey = Math.min(ey, oRange);
                ez = Math.min(ez, oRange);

                return new AABB(
                        origin.x - ex, origin.y - ey, origin.z - ez,
                        origin.x + ex, origin.y + ey, origin.z + ez
                );
            }
        }

        return new AABB(
                origin.x - oRange, origin.y - oRange, origin.z - oRange,
                origin.x + oRange, origin.y + oRange, origin.z + oRange
        );
    }

    @Override
    public float evaluateMultiplier(int enchantmentLevel, Entity spatialReference, Vec3 epicenter, Vec3 targetPos) {
        Vec3 origin = this.getOrigin(enchantmentLevel, spatialReference, epicenter);
        Vec3 vectorToTarget = targetPos.subtract(origin);

        float oRange = this.originRange.calculate(enchantmentLevel);
        if (oRange <= 0.0F) return 0.0F;

        double originDist = this.metric.calculate(vectorToTarget);
        if (originDist > oRange) return 0.0F;

        float originMultiplier = this.originScaling.apply((float) originDist, oRange);
        if (originMultiplier <= 0.0001F) return 0.0F;

        float axialMultiplier = 1.0F;
        float radialMultiplier = 1.0F;

        if (this.axis.isPresent()) {
            Vec3 dir = this.axis.get().resolve(enchantmentLevel, spatialReference);
            if (dir.lengthSqr() > 0.0001D) {
                dir = dir.normalize();
                double dot = vectorToTarget.dot(dir);
                double axialDist = Math.abs(dot);
                double radialDist = this.metric.calculate(vectorToTarget.subtract(dir.scale(dot)));

                float aRange = this.axialRange.map(lv -> lv.calculate(enchantmentLevel)).orElse(oRange);
                float rRange = this.radialRange.map(lv -> lv.calculate(enchantmentLevel)).orElse(oRange);

                if (axialDist > aRange || radialDist > rRange) return 0.0F;

                axialMultiplier = this.axialScaling.apply((float) axialDist, aRange);
                radialMultiplier = this.radialScaling.apply((float) radialDist, rRange);
            }
        }

        return originMultiplier * axialMultiplier * radialMultiplier;
    }

    // region Wither Pattern

    /**
     * Constructs a default {@link FieldTopology} with a constant origin range of {@code 1.0},
     * constant scaling functions, and Euclidean distance metric.
     */
    public FieldTopology() {
        this(LevelBasedValue.constant(1), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), ConstantScalingFunction.NONE, ConstantScalingFunction.NONE, ConstantScalingFunction.NONE, DistanceMetric.EUCLIDEAN);
    }

    /**
     * Returns a copy of this topology with the specified primary origin range.
     *
     * @param originRange The new {@link LevelBasedValue} origin range.
     * @return A new {@link FieldTopology} instance.
     */
    public FieldTopology withOriginRange(LevelBasedValue originRange) {
        return new FieldTopology(originRange, axialRange, radialRange, originOffset, axis, originScaling, axialScaling, radialScaling, metric);
    }

    /**
     * Returns a copy of this topology with the specified axial range limit.
     *
     * @param axialRange The new {@link LevelBasedValue} axial range.
     * @return A new {@link FieldTopology} instance.
     */
    public FieldTopology withAxialRange(LevelBasedValue axialRange) {
        return new FieldTopology(originRange, Optional.of(axialRange), radialRange, originOffset, axis, originScaling, axialScaling, radialScaling, metric);
    }

    /**
     * Returns a copy of this topology with the specified radial range limit.
     *
     * @param radialRange The new {@link LevelBasedValue} radial range.
     * @return A new {@link FieldTopology} instance.
     */
    public FieldTopology withRadialRange(LevelBasedValue radialRange) {
        return new FieldTopology(originRange, axialRange, Optional.of(radialRange), originOffset, axis, originScaling, axialScaling, radialScaling, metric);
    }

    /**
     * Returns a copy of this topology with the specified origin offset vector.
     *
     * @param originOffset The {@link SpatialVector} offset from the volume center.
     * @return A new {@link FieldTopology} instance.
     */
    public FieldTopology withOriginOffset(SpatialVector originOffset) {
        return new FieldTopology(originRange, axialRange, radialRange, Optional.of(originOffset), axis, originScaling, axialScaling, radialScaling, metric);
    }

    /**
     * Returns a copy of this topology with the specified orientation axis vector.
     *
     * @param axis The {@link SpatialVector} defining the primary orientation axis.
     * @return A new {@link FieldTopology} instance.
     */
    public FieldTopology withAxis(SpatialVector axis) {
        return new FieldTopology(originRange, axialRange, radialRange, originOffset, Optional.of(axis), originScaling, axialScaling, radialScaling, metric);
    }

    /**
     * Returns a copy of this topology with the specified origin distance scaling function.
     *
     * @param originScaling The {@link DistanceScalingFunction} for origin distance falloff.
     * @return A new {@link FieldTopology} instance.
     */
    public FieldTopology withOriginScaling(DistanceScalingFunction originScaling) {
        return new FieldTopology(originRange, axialRange, radialRange, originOffset, axis, originScaling, axialScaling, radialScaling, metric);
    }

    /**
     * Returns a copy of this topology with the specified axial distance scaling function.
     *
     * @param axialScaling The {@link DistanceScalingFunction} for axial distance falloff.
     * @return A new {@link FieldTopology} instance.
     */
    public FieldTopology withAxialScaling(DistanceScalingFunction axialScaling) {
        return new FieldTopology(originRange, axialRange, radialRange, originOffset, axis, originScaling, axialScaling, radialScaling, metric);
    }

    /**
     * Returns a copy of this topology with the specified radial distance scaling function.
     *
     * @param radialScaling The {@link DistanceScalingFunction} for radial distance falloff.
     * @return A new {@link FieldTopology} instance.
     */
    public FieldTopology withRadialScaling(DistanceScalingFunction radialScaling) {
        return new FieldTopology(originRange, axialRange, radialRange, originOffset, axis, originScaling, axialScaling, radialScaling, metric);
    }

    /**
     * Returns a copy of this topology with the specified spatial distance metric.
     *
     * @param metric The {@link DistanceMetric} to use.
     * @return A new {@link FieldTopology} instance.
     */
    public FieldTopology withDistanceMetric(DistanceMetric metric) {
        return new FieldTopology(originRange, axialRange, radialRange, originOffset, axis, originScaling, axialScaling, radialScaling, metric);
    }
    // endregion
}