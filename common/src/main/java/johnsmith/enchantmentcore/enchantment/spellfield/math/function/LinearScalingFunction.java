package johnsmith.enchantmentcore.enchantment.spellfield.math.function;

import com.mojang.serialization.MapCodec;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;

public record LinearScalingFunction() implements DistanceScalingFunction {

    public static final String KEY = "linear";

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