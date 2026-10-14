package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

/**
 * Defines the mathematical norm used to measure spatial distance within a {@link Topology}.
 */
public enum DistanceMetric implements StringRepresentable {
    /**
     * Standard Euclidean distance calculation ($L_2$ norm): $\sqrt{x^2 + y^2 + z^2}$.
     * Produces spherical or cylindrical isosurfaces.
     */
    EUCLIDEAN("euclidean") {
        @Override
        public double calculate(Vec3 vector) {
            return vector.length();
        }
    },

    /**
     * Taxicab / grid-based distance calculation ($L_1$ norm): $|x| + |y| + |z|$.
     * Produces octahedral or diamond-shaped isosurfaces.
     */
    MANHATTAN("manhattan") {
        @Override
        public double calculate(Vec3 vector) {
            return Math.abs(vector.x) + Math.abs(vector.y) + Math.abs(vector.z);
        }
    };

    /**
     * Codec for serializing and deserializing {@link DistanceMetric} constants by their lowercase identifier.
     */
    public static final Codec<DistanceMetric> CODEC = StringRepresentable.fromEnum(DistanceMetric::values);

    /** The serialized string identifier of this metric. */
    private final String name;

    DistanceMetric(String name) {
        this.name = name;
    }

    /**
     * Computes the scalar distance norm of the provided displacement vector.
     *
     * @param vector The displacement vector between two points in 3D space.
     * @return The non-negative scalar distance.
     */
    public abstract double calculate(Vec3 vector);

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
}