package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Represents a composite spatial translation offset integrating both absolute global coordinates and dynamic local coordinates.
 */
public record SpatialVector(
        LevelBasedValue x, LevelBasedValue y, LevelBasedValue z,
        LevelBasedValue forward, LevelBasedValue up, LevelBasedValue right
) {
    public static final String X = "x";
    public static final String Y = "y";
    public static final String Z = "z";
    public static final String FORWARD = "forward";
    public static final String UP = "up";
    public static final String RIGHT = "right";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(X, Y, Z, FORWARD, UP, RIGHT);

    public static final Codec<SpatialVector> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.optionalFieldOf(X, LevelBasedValue.constant(0.0F)).forGetter(SpatialVector::x),
            LevelBasedValue.CODEC.optionalFieldOf(Y, LevelBasedValue.constant(0.0F)).forGetter(SpatialVector::y),
            LevelBasedValue.CODEC.optionalFieldOf(Z, LevelBasedValue.constant(0.0F)).forGetter(SpatialVector::z),
            LevelBasedValue.CODEC.optionalFieldOf(FORWARD, LevelBasedValue.constant(0.0F)).forGetter(SpatialVector::forward),
            LevelBasedValue.CODEC.optionalFieldOf(UP, LevelBasedValue.constant(0.0F)).forGetter(SpatialVector::up),
            LevelBasedValue.CODEC.optionalFieldOf(RIGHT, LevelBasedValue.constant(0.0F)).forGetter(SpatialVector::right)
    ).apply(instance, SpatialVector::new));

    /**
     * Null-offset baseline instance representing zero absolute and zero relative displacement.
     */
    public static final SpatialVector ZERO = new SpatialVector(
            LevelBasedValue.constant(0), LevelBasedValue.constant(0), LevelBasedValue.constant(0),
            LevelBasedValue.constant(0), LevelBasedValue.constant(0), LevelBasedValue.constant(0)
    );

    /**
     * Computes the final spatial vector by summing the absolute Cartesian offsets and the entity-relative orientational offsets.
     *
     * @param enchantmentLevel The level of the spell field enchantment executing the resolution.
     * @param spatialReference The entity dictating the local coordinate reference frame.
     * @return The final composite transformation vector.
     */
    public Vec3 resolve(int enchantmentLevel, Entity spatialReference) {
        double absX = this.x.calculate(enchantmentLevel);
        double absY = this.y.calculate(enchantmentLevel);
        double absZ = this.z.calculate(enchantmentLevel);
        Vec3 absolute = new Vec3(absX, absY, absZ);

        double f = this.forward.calculate(enchantmentLevel);
        double u = this.up.calculate(enchantmentLevel);
        double r = this.right.calculate(enchantmentLevel);

        Vec3 vF = FieldAxis.LONGITUDINAL.getReferenceAxis(spatialReference).scale(f);
        Vec3 vU = FieldAxis.VERTICAL.getReferenceAxis(spatialReference).scale(u);
        Vec3 vR = FieldAxis.LATERAL.getReferenceAxis(spatialReference).scale(r);

        return absolute.add(vF).add(vU).add(vR);
    }
}