package johnsmith.enchantmentcore.enchantment.spellfield.math.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;

public record ExponentialScalingFunction(
        float decayRate
) implements DistanceScalingFunction {

    public static final String DECAY_RATE = "decay_rate";

    public static final String KEY = "exponential";

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