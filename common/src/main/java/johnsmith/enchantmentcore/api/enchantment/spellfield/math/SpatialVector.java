package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * Represents a composite 3D spatial vector combining world-aligned Cartesian coordinates
 * ($x, y, z$) with entity-relative local basis coordinates ($\text{forward}, \text{up}, \text{right}$).
 *
 * @param x       The {@link LevelBasedValue} offset along the global $+X$ (East) axis.
 * @param y       The {@link LevelBasedValue} offset along the global $+Y$ (Up) axis.
 * @param z       The {@link LevelBasedValue} offset along the global $+Z$ (South) axis.
 * @param forward The {@link LevelBasedValue} offset along the reference entity's {@link FieldAxis#LONGITUDINAL} view vector.
 * @param up      The {@link LevelBasedValue} offset along the reference entity's {@link FieldAxis#VERTICAL} up vector.
 * @param right   The {@link LevelBasedValue} offset along the reference entity's {@link FieldAxis#LATERAL} right vector.
 */
public record SpatialVector(
        LevelBasedValue x, LevelBasedValue y, LevelBasedValue z,
        LevelBasedValue forward, LevelBasedValue up, LevelBasedValue right
) {
    /** JSON field key for the global X component. */
    public static final String X = "x";

    /** JSON field key for the global Y component. */
    public static final String Y = "y";

    /** JSON field key for the global Z component. */
    public static final String Z = "z";

    /** JSON field key for the local forward component. */
    public static final String FORWARD = "forward";

    /** JSON field key for the local up component. */
    public static final String UP = "up";

    /** JSON field key for the local right component. */
    public static final String RIGHT = "right";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(X, Y, Z, FORWARD, UP, RIGHT);

    /**
     * Codec for serializing and deserializing {@link SpatialVector} instances from JSON.
     */
    public static final Codec<SpatialVector> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.optionalFieldOf(X, LevelBasedValue.constant(0.0F)).forGetter(SpatialVector::x),
            LevelBasedValue.CODEC.optionalFieldOf(Y, LevelBasedValue.constant(0.0F)).forGetter(SpatialVector::y),
            LevelBasedValue.CODEC.optionalFieldOf(Z, LevelBasedValue.constant(0.0F)).forGetter(SpatialVector::z),
            LevelBasedValue.CODEC.optionalFieldOf(FORWARD, LevelBasedValue.constant(0.0F)).forGetter(SpatialVector::forward),
            LevelBasedValue.CODEC.optionalFieldOf(UP, LevelBasedValue.constant(0.0F)).forGetter(SpatialVector::up),
            LevelBasedValue.CODEC.optionalFieldOf(RIGHT, LevelBasedValue.constant(0.0F)).forGetter(SpatialVector::right)
    ).apply(instance, SpatialVector::new));

    /**
     * Baseline zero-displacement instance with all global and local components set to {@code 0.0}.
     */
    public static final SpatialVector ZERO = new SpatialVector(
            LevelBasedValue.constant(0), LevelBasedValue.constant(0), LevelBasedValue.constant(0),
            LevelBasedValue.constant(0), LevelBasedValue.constant(0), LevelBasedValue.constant(0)
    );

    /**
     * Evaluates and sums the global Cartesian offsets and the entity-relative orientational basis offsets
     * into a single world-space {@link Vec3}.
     *
     * @param enchantmentLevel The active level of the enchantment executing the resolution.
     * @param spatialReference The entity dictating the local coordinate reference frame, or {@code null}.
     * @return The resolved composite displacement vector in world coordinates.
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