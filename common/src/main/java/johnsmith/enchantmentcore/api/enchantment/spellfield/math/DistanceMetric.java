package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;

/**
 * Defines the mathematical algorithm utilized to calculate spatial distance.
 */
public enum DistanceMetric implements StringRepresentable {
    /**
     * Standard linear distance calculation (L2 norm).
     */
    EUCLIDEAN("euclidean") {
        @Override
        public double calculate(Vec3 vector) {
            return vector.length();
        }
    },
    /**
     * Grid-based distance calculation summing the absolute differences of Cartesian coordinates (L1 norm).
     */
    MANHATTAN("manhattan") {
        @Override
        public double calculate(Vec3 vector) {
            return Math.abs(vector.x) + Math.abs(vector.y) + Math.abs(vector.z);
        }
    };

    public static final Codec<DistanceMetric> CODEC = StringRepresentable.fromEnum(DistanceMetric::values);

    private final String name;

    DistanceMetric(String name) {
        this.name = name;
    }

    /**
     * Executes the distance calculation algorithm on the provided vector.
     *
     * @param vector The vector representing the difference between two spatial points.
     * @return The calculated scalar distance.
     */
    public abstract double calculate(Vec3 vector);

    @Override
    public String getSerializedName() {
        return this.name;
    }
}