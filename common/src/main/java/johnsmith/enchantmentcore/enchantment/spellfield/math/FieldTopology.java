package johnsmith.enchantmentcore.enchantment.spellfield.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceMetric;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpatialVector;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.Topology;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.math.function.ConstantScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;
import johnsmith.enchantmentcore.registry.EnchantmentCoreRegistries;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
    public static final String ORIGIN_RANGE = "origin_range";
    public static final String AXIAL_RANGE = "axial_range";
    public static final String RADIAL_RANGE = "radial_range";
    public static final String ORIGIN_OFFSET = "origin_offset";
    public static final String AXIS = "axis";
    public static final String ORIGIN_SCALING = "origin_scaling";
    public static final String AXIAL_SCALING = "axial_scaling";
    public static final String RADIAL_SCALING = "radial_scaling";
    public static final String METRIC = "metric";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> {
        Set<String> set = new HashSet<>(Set.of(ORIGIN_RANGE, AXIAL_RANGE, RADIAL_RANGE));
        set.addAll(SpatialVector.KEY_PROVIDER.getLevelBasedKeys());
        return set;
    };

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

    public Vec3 getOrigin(int enchantmentLevel, Entity spatialReference, Vec3 epicenter) {
        return epicenter.add(this.originOffset.orElse(SpatialVector.ZERO).resolve(enchantmentLevel, spatialReference));
    }

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
}