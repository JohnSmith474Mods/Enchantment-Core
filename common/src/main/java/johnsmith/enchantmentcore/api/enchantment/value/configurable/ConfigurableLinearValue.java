package johnsmith.enchantmentcore.api.enchantment.value.configurable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.listener.FloatPropertyListener;
import johnsmith.enchantmentcore.api.config.ConfigReference;

import net.minecraft.world.item.enchantment.LevelBasedValue;

import org.jetbrains.annotations.NotNull;

/**
 * Dynamic configuration-backed counterpart to {@link LevelBasedValue.Linear}.
 * <p>
 * Evaluates $\text{base} + \text{perLevel} \cdot (\text{level} - 1)$, where both {@code base} and
 * {@code perLevel} are bound to live float configuration properties resolved via {@link ConfigReference}.
 */
public final class ConfigurableLinearValue implements LevelBasedValue {
    /** Registry path identifier for this level based value. */
    public static final String KEY = "configurable_linear";

    /**
     * MapCodec for serializing and deserializing {@link ConfigurableLinearValue} instances from JSON.
     */
    public static final MapCodec<ConfigurableLinearValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ConfigReference.CODEC.fieldOf("base_config").forGetter(ConfigurableLinearValue::baseReference),
            Codec.FLOAT.fieldOf("base_default").forGetter(ConfigurableLinearValue::baseDefault),
            Codec.FLOAT.optionalFieldOf("base_min", -Float.MAX_VALUE).forGetter(ConfigurableLinearValue::baseMin),
            Codec.FLOAT.optionalFieldOf("base_max", Float.MAX_VALUE).forGetter(ConfigurableLinearValue::baseMax),
            ConfigReference.CODEC.fieldOf("per_level_config").forGetter(ConfigurableLinearValue::perLevelReference),
            Codec.FLOAT.fieldOf("per_level_default").forGetter(ConfigurableLinearValue::perLevelDefault),
            Codec.FLOAT.optionalFieldOf("per_level_min", -Float.MAX_VALUE).forGetter(ConfigurableLinearValue::perLevelMin),
            Codec.FLOAT.optionalFieldOf("per_level_max", Float.MAX_VALUE).forGetter(ConfigurableLinearValue::perLevelMax)
    ).apply(instance, ConfigurableLinearValue::new));

    /** Configuration reference for the base value at level 1. */
    private final ConfigReference baseReference;

    /** Default fallback for the base value. */
    private final Float baseDefault;

    /** Minimum allowed configuration value for the base property. */
    private final Float baseMin;

    /** Maximum allowed configuration value for the base property. */
    private final Float baseMax;

    /** Configuration reference for the increment added per level above the first. */
    private final ConfigReference perLevelReference;

    /** Default fallback for the per-level increment. */
    private final Float perLevelDefault;

    /** Minimum allowed configuration value for the per-level property. */
    private final Float perLevelMin;

    /** Maximum allowed configuration value for the per-level property. */
    private final Float perLevelMax;

    /** Live property listener for the base value. */
    private final PropertyWrapper baseListener;

    /** Live property listener for the per-level increment. */
    private final PropertyWrapper perLevelListener;

    /**
     * Constructs a {@link ConfigurableLinearValue} bound to the specified base and per-level configuration references.
     *
     * @param baseReference     The {@link ConfigReference} for the base value.
     * @param baseDefault       The default base value.
     * @param baseMin           The minimum bound for the base configuration property.
     * @param baseMax           The maximum bound for the base configuration property.
     * @param perLevelReference The {@link ConfigReference} for the per-level increment.
     * @param perLevelDefault   The default per-level increment.
     * @param perLevelMin       The minimum bound for the per-level configuration property.
     * @param perLevelMax       The maximum bound for the per-level configuration property.
     */
    public ConfigurableLinearValue(ConfigReference baseReference, Float baseDefault, Float baseMin, Float baseMax,
                                   ConfigReference perLevelReference, Float perLevelDefault, Float perLevelMin, Float perLevelMax) {
        this.baseReference = baseReference;
        this.baseDefault = baseDefault;
        this.baseMin = baseMin;
        this.baseMax = baseMax;
        this.perLevelReference = perLevelReference;
        this.perLevelDefault = perLevelDefault;
        this.perLevelMin = perLevelMin;
        this.perLevelMax = perLevelMax;

        this.baseListener = new PropertyWrapper(baseReference.resolve(), baseDefault, baseMin, baseMax);
        this.perLevelListener = new PropertyWrapper(perLevelReference.resolve(), perLevelDefault, perLevelMin, perLevelMax);
    }

    /**
     * Calculates $\text{base} + \text{perLevel} \cdot (\text{level} - 1)$ using the active configuration values.
     *
     * @param level The active level of the enchantment.
     * @return The calculated linear float value.
     */
    @Override
    public float calculate(int level) {
        return this.baseListener.getValue() + this.perLevelListener.getValue() * (float) (level - 1);
    }

    /**
     * Returns the {@link ConfigReference} for the base value.
     *
     * @return The base configuration reference.
     */
    public ConfigReference baseReference() { return this.baseReference; }

    /**
     * Returns the default fallback for the base value.
     *
     * @return The default base float value.
     */
    public Float baseDefault() { return this.baseDefault; }

    /**
     * Returns the minimum allowed value for the base configuration property.
     *
     * @return The minimum base float bound.
     */
    public Float baseMin() { return this.baseMin; }

    /**
     * Returns the maximum allowed value for the base configuration property.
     *
     * @return The maximum base float bound.
     */
    public Float baseMax() { return this.baseMax; }

    /**
     * Returns the {@link ConfigReference} for the per-level increment.
     *
     * @return The per-level configuration reference.
     */
    public ConfigReference perLevelReference() { return this.perLevelReference; }

    /**
     * Returns the default fallback for the per-level increment.
     *
     * @return The default per-level float value.
     */
    public Float perLevelDefault() { return this.perLevelDefault; }

    /**
     * Returns the minimum allowed value for the per-level configuration property.
     *
     * @return The minimum per-level float bound.
     */
    public Float perLevelMin() { return this.perLevelMin; }

    /**
     * Returns the maximum allowed value for the per-level configuration property.
     *
     * @return The maximum per-level float bound.
     */
    public Float perLevelMax() { return this.perLevelMax; }

    @Override
    public @NotNull MapCodec<? extends LevelBasedValue> codec() {
        return CODEC;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ConfigurableLinearValue) obj;
        return Objects.equals(this.baseReference, that.baseReference) &&
                Objects.equals(this.perLevelReference, that.perLevelReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.baseReference, this.perLevelReference);
    }

    /**
     * Internal wrapper bridging the protected resolution logic of {@link FloatPropertyListener}
     * with the calculation pipeline.
     */
    private static final class PropertyWrapper extends FloatPropertyListener {
        public PropertyWrapper(ConfigDescription description, Float defaultValue, Float min, Float max) {
            super(description, defaultValue, min, max);
        }

        public float getValue() {
            this.resolveProperty(false);
            return (Float) this.cachedProperty.get();
        }
    }
}