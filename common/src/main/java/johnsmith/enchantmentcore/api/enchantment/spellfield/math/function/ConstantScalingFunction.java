package johnsmith.enchantmentcore.api.enchantment.spellfield.math.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;

/**
 * A {@link DistanceScalingFunction} that returns a uniform constant multiplier regardless of distance
 * (f(d) = value).
 *
 * @param value The constant scalar multiplier returned across the entire radius.
 */
public record ConstantScalingFunction(
        float value
) implements DistanceScalingFunction {
    /** JSON field key for the constant scalar value. */
    public static final String VALUE = "value";

    /** Registry path identifier for this distance scaling function. */
    public static final String KEY = "constant";

    /** Shared identity instance returning a uniform multiplier of {@code 1.0F} (no distance falloff). */
    public static final ConstantScalingFunction NONE = new ConstantScalingFunction(1.0f);

    /**
     * MapCodec for serializing and deserializing {@link ConstantScalingFunction} instances from JSON.
     */
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