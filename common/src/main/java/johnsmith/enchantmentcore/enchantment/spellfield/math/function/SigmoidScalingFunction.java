package johnsmith.enchantmentcore.enchantment.spellfield.math.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;

public record SigmoidScalingFunction(
        float steepness,
        float midpointRatio
) implements DistanceScalingFunction {

    public static final String STEEPNESS = "steepness";
    public static final String MIDPOINT_RATIO = "midpoint_ratio";

    public static final String KEY = "sigmoid";

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