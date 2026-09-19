package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import com.mojang.serialization.MapCodec;

/**
 * Defines a mathematical function mapping a spatial distance to an effect scalar multiplier.
 */
public interface DistanceScalingFunction {
    /**
     * Evaluates the scaling function.
     *
     * @param distance  The evaluated distance from the spatial origin.
     * @param maxRadius The defined maximum operational radius of the topological component.
     * @return The calculated multiplier scalar, bounded between 0.0 and 1.0.
     */
    float apply(float distance, float maxRadius);

    /**
     * Retrieves the codec responsible for serializing and deserializing this function.
     *
     * @return The map codec instance.
     */
    MapCodec<? extends DistanceScalingFunction> codec();
}