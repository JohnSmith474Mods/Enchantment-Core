package johnsmith.enchantmentcore.api.enchantment.value.configurable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

import johnsmith.configoverhauled.api.listener.FloatPropertyListener;
import johnsmith.enchantmentcore.api.config.ConfigReference;

import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.LevelBasedValue;

import org.jetbrains.annotations.NotNull;

/**
 * Dynamic configuration-backed counterpart to {@link LevelBasedValue.LevelsSquared}.
 * <p>
 * Evaluates $\text{level}^2 + \text{added}$, where the additive constant is bound to a live float
 * configuration property resolved via {@link ConfigReference}.
 */
public final class ConfigurableLevelsSquaredValue extends FloatPropertyListener implements LevelBasedValue {
    /** Registry path identifier for this level based value. */
    public static final String KEY = "configurable_levels_squared";

    /**
     * MapCodec for serializing and deserializing {@link ConfigurableLevelsSquaredValue} instances from JSON.
     */
    public static final MapCodec<ConfigurableLevelsSquaredValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ConfigReference.CODEC.fieldOf("config").forGetter(ConfigurableLevelsSquaredValue::reference),
            Codec.FLOAT.fieldOf("default_added").forGetter(ConfigurableLevelsSquaredValue::defaultValue),
            Codec.FLOAT.optionalFieldOf("min", -Float.MAX_VALUE).forGetter(ConfigurableLevelsSquaredValue::min),
            Codec.FLOAT.optionalFieldOf("max", Float.MAX_VALUE).forGetter(ConfigurableLevelsSquaredValue::max)
    ).apply(instance, ConfigurableLevelsSquaredValue::new));

    /** The configuration reference pointing to the additive float property. */
    private final ConfigReference reference;

    /**
     * Constructs a {@link ConfigurableLevelsSquaredValue} bound to the given configuration reference.
     *
     * @param reference    The {@link ConfigReference} identifying the additive constant configuration property.
     * @param defaultValue The default fallback value for the added constant.
     * @param min          The minimum allowed value for the generated configuration property.
     * @param max          The maximum allowed value for the generated configuration property.
     */
    public ConfigurableLevelsSquaredValue(ConfigReference reference, Float defaultValue, Float min, Float max) {
        super(reference.resolve(), defaultValue, min, max);
        this.reference = reference;
    }

    /**
     * Calculates $\text{level}^2 + \text{added}$ using the currently configured additive float value.
     *
     * @param level The active level of the enchantment.
     * @return The calculated float value.
     */
    @Override
    public float calculate(int level) {
        this.resolveProperty(false);
        return (float) Mth.square(level) + (Float) this.cachedProperty.get();
    }

    /**
     * Returns the {@link ConfigReference} targeting the additive constant property.
     *
     * @return The configuration reference.
     */
    public ConfigReference reference() {
        return this.reference;
    }

    /**
     * Returns the default fallback value for the additive constant.
     *
     * @return The default added float value.
     */
    public Float defaultValue() {
        return this.defaultValue;
    }

    /**
     * Returns the lower bound constraint of the configuration property.
     *
     * @return The minimum allowed float value.
     */
    public Float min() {
        return this.lowerBound;
    }

    /**
     * Returns the upper bound constraint of the configuration property.
     *
     * @return The maximum allowed float value.
     */
    public Float max() {
        return this.upperBound;
    }

    @Override
    public @NotNull MapCodec<? extends LevelBasedValue> codec() {
        return CODEC;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ConfigurableLevelsSquaredValue) obj;
        return Objects.equals(this.reference, that.reference) &&
                Float.compare(this.defaultValue, that.defaultValue) == 0 &&
                Float.compare(this.lowerBound, that.lowerBound) == 0 &&
                Float.compare(this.upperBound, that.upperBound) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.reference, this.defaultValue, this.lowerBound, this.upperBound);
    }

    @Override
    public String toString() {
        return "ConfigurableLevelsSquaredValue[reference=" + this.reference +
                ", defaultValue=" + this.defaultValue +
                ", min=" + this.lowerBound +
                ", max=" + this.upperBound + "]";
    }
}