package johnsmith.enchantmentcore.enchantment.spellfield.math.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;

public record ConstantScalingFunction(
        float value
) implements DistanceScalingFunction {

    public static final String VALUE = "value";

    public static final String KEY = "constant";

    public static final ConstantScalingFunction NONE = new ConstantScalingFunction(1.0f);

    public static final MapCodec<ConstantScalingFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf(VALUE, 1.0F).forGetter(ConstantScalingFunction::value)
    ).apply(instance, ConstantScalingFunction::new));

    @Override
    public float apply(float distance, float maxRadius) {
        return this.value;
    }

    @Override
    public MapCodec<? extends DistanceScalingFunction> codec() {
        return CODEC;
    }
}