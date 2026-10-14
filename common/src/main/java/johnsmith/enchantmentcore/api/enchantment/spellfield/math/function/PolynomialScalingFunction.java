package johnsmith.enchantmentcore.api.enchantment.spellfield.math.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;

/**
 * A {@link DistanceScalingFunction} that attenuates effect intensity according to a power curve of
 * normalized distance:
 * <br>
 * f(d, R) = max(0, 1 - (d / R)<sup>p</sup>)
 * <br>
 * where <em>p</em> is {@link #degree()}.
 *
 * @param degree The polynomial exponent <em>p</em> (e.g., <em>p</em> = 2.0 yields quadratic falloff).
 */
public record PolynomialScalingFunction(
        float degree
) implements DistanceScalingFunction {
    /** JSON field key for the polynomial exponent degree. */
    public static final String DEGREE = "degree";

    /** Registry path identifier for this distance scaling function. */
    public static final String KEY = "polynomial";

    /**
     * MapCodec for serializing and deserializing {@link PolynomialScalingFunction} instances from JSON.
     */
    public static final MapCodec<PolynomialScalingFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf(DEGREE, 1.0f).forGetter(PolynomialScalingFunction::degree)
    ).apply(instance, PolynomialScalingFunction::new));

    @Override
    public float apply(float distance, float maxRadius) {
        if (maxRadius <= 0.0F) return 1.0F;
        float normalized = distance / maxRadius;
        return Math.max(0.0F, 1.0F - (float) Math.pow(normalized, this.degree));
    }

    @Override
    public MapCodec<? extends DistanceScalingFunction> codec() {
        return CODEC;
    }
}