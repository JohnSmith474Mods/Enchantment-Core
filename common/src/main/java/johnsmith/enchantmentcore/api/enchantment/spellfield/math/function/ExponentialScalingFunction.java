package johnsmith.enchantmentcore.api.enchantment.spellfield.math.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;

/**
 * A {@link DistanceScalingFunction} that attenuates effect intensity exponentially with normalized distance:
 * <br>
 * f(d, R) = e<sup>-λ · (d / R)</sup>
 * <br>
 * where <em>λ</em> is {@link #decayRate()}.
 *
 * @param decayRate The exponential decay constant <em>λ</em> governing how rapidly intensity drops from the origin.
 */
public record ExponentialScalingFunction(
        float decayRate
) implements DistanceScalingFunction {
    /** JSON field key for the exponential decay rate $\lambda$. */
    public static final String DECAY_RATE = "decay_rate";

    /** Registry path identifier for this distance scaling function. */
    public static final String KEY = "exponential";

    /**
     * MapCodec for serializing and deserializing {@link ExponentialScalingFunction} instances from JSON.
     */
    public static final MapCodec<ExponentialScalingFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf(DECAY_RATE).forGetter(ExponentialScalingFunction::decayRate)
    ).apply(instance, ExponentialScalingFunction::new));

    @Override
    public float apply(float distance, float maxRadius) {
        if (maxRadius <= 0.0F) return 1.0F;
        return (float) Math.exp(-this.decayRate * (distance / maxRadius));
    }

    @Override
    public MapCodec<? extends DistanceScalingFunction> codec() {
        return CODEC;
    }
}