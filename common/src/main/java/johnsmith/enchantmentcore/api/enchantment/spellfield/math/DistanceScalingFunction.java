package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import com.mojang.serialization.MapCodec;

/**
 * Contract for a mathematical falloff function that maps a spatial distance from a topological origin
 * or axis to a normalized effect intensity scalar.
 */
public interface DistanceScalingFunction {
    /**
     * Evaluates the distance scaling function at the given distance relative to {@code maxRadius}.
     *
     * @param distance  The measured distance from the spatial origin or reference axis.
     * @param maxRadius The maximum operational radius of the topological component.
     * @return The calculated intensity multiplier scalar (typically bounded in $[0.0, 1.0]$).
     */
    float apply(float distance, float maxRadius);

    /**
     * Retrieves the {@link MapCodec} responsible for serializing and deserializing this function type.
     *
     * @return The registered {@link MapCodec} instance.
     */
    MapCodec<? extends DistanceScalingFunction> codec();
}