package johnsmith.enchantmentcore.api.enchantment.spellfield.math.function;

import com.mojang.serialization.MapCodec;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;

/**
 * A {@link DistanceScalingFunction} that attenuates effect intensity linearly from 1.0 at the origin
 * to 0.0 at {@code maxRadius}:
 * <br>
 * f(d, R) = max(0, 1 - (d / R))
 */
public record LinearScalingFunction() implements DistanceScalingFunction {
    /** Registry path identifier for this distance scaling function. */
    public static final String KEY = "linear";

    /**
     * Singleton unit MapCodec for serializing and deserializing {@link LinearScalingFunction}.
     */
    public static final MapCodec<LinearScalingFunction> CODEC = MapCodec.unit(new LinearScalingFunction());

    @Override
    public float apply(float distance, float maxRadius) {
        if (maxRadius <= 0.0F) return 1.0F;
        return Math.max(0.0F, 1.0F - (distance / maxRadius));
    }

    @Override
    public MapCodec<? extends DistanceScalingFunction> codec() {
        return CODEC;
    }
}