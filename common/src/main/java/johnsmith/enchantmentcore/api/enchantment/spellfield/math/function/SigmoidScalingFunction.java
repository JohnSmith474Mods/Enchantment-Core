package johnsmith.enchantmentcore.api.enchantment.spellfield.math.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;

/**
 * A {@link DistanceScalingFunction} that attenuates effect intensity along a logistic S-curve:
 * <br>
 * f(d, R) = 1 / (1 + e<sup>k · (d - m · R)</sup>)
 * <br>
 * where <em>k</em> is {@link #steepness()} and <em>m</em> is {@link #midpointRatio()}.
 *
 * @param steepness     The logistic growth/decay rate <em>k</em> controlling how sharply the transition occurs.
 * @param midpointRatio The fraction <em>m</em> of {@code maxRadius} at which the multiplier crosses 0.5 (50% intensity).
 */
public record SigmoidScalingFunction(
        float steepness,
        float midpointRatio
) implements DistanceScalingFunction {
    /** JSON field key for the logistic steepness $k$. */
    public static final String STEEPNESS = "steepness";

    /** JSON field key for the normalized inflection midpoint ratio $m$. */
    public static final String MIDPOINT_RATIO = "midpoint_ratio";

    /** Registry path identifier for this distance scaling function. */
    public static final String KEY = "sigmoid";

    /**
     * MapCodec for serializing and deserializing {@link SigmoidScalingFunction} instances from JSON.
     */
    public static final MapCodec<SigmoidScalingFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf(STEEPNESS, 1.0f).forGetter(SigmoidScalingFunction::steepness),
            Codec.FLOAT.optionalFieldOf(MIDPOINT_RATIO, 1.0f).forGetter(SigmoidScalingFunction::midpointRatio)
    ).apply(instance, SigmoidScalingFunction::new));

    @Override
    public float apply(float distance, float maxRadius) {
        if (maxRadius <= 0.0F) return 1.0F;

        // Calculate where the S-curve crosses 50% power
        float absoluteMidpoint = maxRadius * this.midpointRatio;

        // The sigmoid math
        return (float) (1.0 / (1.0 + Math.exp(this.steepness * (distance - absoluteMidpoint))));
    }

    @Override
    public MapCodec<? extends DistanceScalingFunction> codec() {
        return CODEC;
    }
}