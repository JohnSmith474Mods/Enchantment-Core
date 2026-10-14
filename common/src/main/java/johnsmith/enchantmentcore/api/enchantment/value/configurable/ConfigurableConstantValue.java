package johnsmith.enchantmentcore.api.enchantment.value.configurable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

import johnsmith.configoverhauled.api.listener.FloatPropertyListener;
import johnsmith.enchantmentcore.api.config.ConfigReference;

import net.minecraft.world.item.enchantment.LevelBasedValue;
import org.jetbrains.annotations.NotNull;

/**
 * Dynamic configuration-backed counterpart to {@link LevelBasedValue.Constant}.
 * <p>
 * Extends {@link FloatPropertyListener} to lazily register and cache a live float configuration
 * property resolved via {@link ConfigReference}, returning its current value regardless of enchantment level.
 */
public final class ConfigurableConstantValue extends FloatPropertyListener implements LevelBasedValue {
    /** Registry path identifier for this level based value. */
    public static final String KEY = "configurable_constant";

    /**
     * MapCodec for serializing and deserializing {@link ConfigurableConstantValue} instances from JSON.
     */
    public static final MapCodec<ConfigurableConstantValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ConfigReference.CODEC.fieldOf("config").forGetter(ConfigurableConstantValue::reference),
            Codec.FLOAT.fieldOf("default_value").forGetter(ConfigurableConstantValue::defaultValue),
            Codec.FLOAT.optionalFieldOf("min", -Float.MAX_VALUE).forGetter(ConfigurableConstantValue::min),
            Codec.FLOAT.optionalFieldOf("max", Float.MAX_VALUE).forGetter(ConfigurableConstantValue::max)
    ).apply(instance, ConfigurableConstantValue::new));

    /** The configuration reference pointing to the backing float property. */
    private final ConfigReference reference;

    /**
     * Constructs a {@link ConfigurableConstantValue} bound to the given configuration reference.
     *
     * @param reference    The {@link ConfigReference} identifying the target configuration property.
     * @param defaultValue The fallback constant float value.
     * @param min          The minimum allowed value for the generated configuration property.
     * @param max          The maximum allowed value for the generated configuration property.
     */
    public ConfigurableConstantValue(ConfigReference reference, Float defaultValue, Float min, Float max) {
        super(reference.resolve(), defaultValue, min, max);
        this.reference = reference;
    }

    /**
     * Resolves and returns the current float value from the bound configuration property.
     *
     * @param level The active level of the enchantment (unused for constant values).
     * @return The configured constant float value.
     */
    @Override
    public float calculate(int level) {
        this.resolveProperty(false);
        return (Float) this.cachedProperty.get();
    }

    /**
     * Returns the {@link ConfigReference} targeting the backing configuration property.
     *
     * @return The configuration reference.
     */
    public ConfigReference reference() {
        return this.reference;
    }

    /**
     * Returns the default fallback float value.
     *
     * @return The default value.
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
        var that = (ConfigurableConstantValue) obj;
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
        return "ConfigurableConstantValue[reference=" + this.reference +
                ", defaultValue=" + this.defaultValue +
                ", min=" + this.lowerBound +
                ", max=" + this.upperBound + "]";
    }
}