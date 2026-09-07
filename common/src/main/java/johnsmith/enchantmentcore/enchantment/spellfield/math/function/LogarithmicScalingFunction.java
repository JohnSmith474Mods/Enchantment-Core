package johnsmith.enchantmentcore.enchantment.spellfield.math.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;

public record LogarithmicScalingFunction(
        float steepness
) implements DistanceScalingFunction {

    public static final String STEEPNESS = "steepness";

    public static final String KEY = "logarithmic";

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