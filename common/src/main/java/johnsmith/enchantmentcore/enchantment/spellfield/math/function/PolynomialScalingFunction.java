package johnsmith.enchantmentcore.enchantment.spellfield.math.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;

public record PolynomialScalingFunction(
        float degree
) implements DistanceScalingFunction {

    public static final String DEGREE = "degree";

    public static final String KEY = "polynomial";

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