package johnsmith.enchantmentcore.api.enchantment.spellfield.math.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;

/**
 * A {@link DistanceScalingFunction} that attenuates effect intensity along a normalized logarithmic curve:
 * <br>
 * f(d, R) = max(0, 1 - ln(1 + k · (d / R)) / ln(1 + k))
 * <br>
 * where <em>k</em> is {@link #steepness()}.
 *
 * @param steepness The positive curvature parameter <em>k</em> controlling the initial drop-off rate near the origin.
 */
public record LogarithmicScalingFunction(
        float steepness
) implements DistanceScalingFunction {
    /** JSON field key for the logarithmic steepness parameter. */
    public static final String STEEPNESS = "steepness";

    /** Registry path identifier for this distance scaling function. */
    public static final String KEY = "logarithmic";

    /**
     * MapCodec for serializing and deserializing {@link LogarithmicScalingFunction} instances from JSON.
     */
    public static final MapCodec<LogarithmicScalingFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf(STEEPNESS, 1.0F).forGetter(LogarithmicScalingFunction::steepness)
    ).apply(instance, LogarithmicScalingFunction::new));

    @Override
    public float apply(float distance, float maxRadius) {
        if (maxRadius <= 0.0F || this.steepness <= 0.0F) return 1.0F;

        float normalized = distance / maxRadius;
        double numerator = Math.log(1.0 + this.steepness * normalized);
        double denominator = Math.log(1.0 + this.steepness);

        return Math.max(0.0F, 1.0F - (float) (numerator / denominator));
    }

    @Override
    public MapCodec<? extends DistanceScalingFunction> codec() {
        return CODEC;
    }
}