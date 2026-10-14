package johnsmith.enchantmentcore.api.enchantment.value.configurable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.listener.FloatPropertyListener;
import johnsmith.enchantmentcore.api.config.ConfigReference;
import johnsmith.enchantmentcore.api.enchantment.value.PolynomialValue;

import net.minecraft.world.item.enchantment.LevelBasedValue;

import org.jetbrains.annotations.NotNull;

/**
 * Dynamic configuration-backed counterpart to {@link PolynomialValue}.
 * <p>
 * Evaluates $\text{offset} + \text{scale} \cdot (\text{level} + \text{levelOffset})^{\text{power}}$,
 * where {@code scale}, {@code power}, {@code offset}, and {@code levelOffset} are each bound to live
 * float configuration properties resolved via {@link ConfigReference}.
 */
public final class ConfigurablePolynomialValue implements LevelBasedValue {
    /** Registry path identifier for this level based value. */
    public static final String KEY = "configurable_polynomial";

    /**
     * MapCodec for serializing and deserializing {@link ConfigurablePolynomialValue} instances from JSON.
     */
    public static final MapCodec<ConfigurablePolynomialValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ConfigReference.CODEC.fieldOf("scale_config").forGetter(ConfigurablePolynomialValue::scaleReference),
            Codec.FLOAT.fieldOf("scale_default").forGetter(ConfigurablePolynomialValue::scaleDefault),
            ConfigReference.CODEC.fieldOf("power_config").forGetter(ConfigurablePolynomialValue::powerReference),
            Codec.FLOAT.fieldOf("power_default").forGetter(ConfigurablePolynomialValue::powerDefault),
            ConfigReference.CODEC.fieldOf("offset_config").forGetter(ConfigurablePolynomialValue::offsetReference),
            Codec.FLOAT.fieldOf("offset_default").forGetter(ConfigurablePolynomialValue::offsetDefault),
            ConfigReference.CODEC.fieldOf("level_offset_config").forGetter(ConfigurablePolynomialValue::levelOffsetReference),
            Codec.FLOAT.fieldOf("level_offset_default").forGetter(ConfigurablePolynomialValue::levelOffsetDefault)
    ).apply(instance, ConfigurablePolynomialValue::new));

    /** Configuration reference for the polynomial scale coefficient. */
    private final ConfigReference scaleReference;

    /** Default fallback for the scale coefficient. */
    private final Float scaleDefault;

    /** Configuration reference for the polynomial exponent. */
    private final ConfigReference powerReference;

    /** Default fallback for the polynomial exponent. */
    private final Float powerDefault;

    /** Configuration reference for the additive constant offset. */
    private final ConfigReference offsetReference;

    /** Default fallback for the additive constant offset. */
    private final Float offsetDefault;

    /** Configuration reference for the pre-exponent enchantment level offset. */
    private final ConfigReference levelOffsetReference;

    /** Default fallback for the pre-exponent enchantment level offset. */
    private final Float levelOffsetDefault;

    /** Live property listener for the scale coefficient. */
    private final PropertyWrapper scaleListener;

    /** Live property listener for the exponent. */
    private final PropertyWrapper powerListener;

    /** Live property listener for the additive offset. */
    private final PropertyWrapper offsetListener;

    /** Live property listener for the level offset. */
    private final PropertyWrapper levelOffsetListener;

    /**
     * Constructs a {@link ConfigurablePolynomialValue} bound to the specified configuration references.
     *
     * @param scaleReference       The {@link ConfigReference} for the scale coefficient.
     * @param scaleDefault         The default scale coefficient.
     * @param powerReference       The {@link ConfigReference} for the exponent power.
     * @param powerDefault         The default exponent power.
     * @param offsetReference      The {@link ConfigReference} for the additive output offset.
     * @param offsetDefault        The default additive output offset.
     * @param levelOffsetReference The {@link ConfigReference} for the pre-exponent level offset.
     * @param levelOffsetDefault   The default pre-exponent level offset.
     */
    public ConfigurablePolynomialValue(ConfigReference scaleReference, Float scaleDefault,
                                       ConfigReference powerReference, Float powerDefault,
                                       ConfigReference offsetReference, Float offsetDefault,
                                       ConfigReference levelOffsetReference, Float levelOffsetDefault) {
        this.scaleReference = scaleReference;
        this.scaleDefault = scaleDefault;
        this.powerReference = powerReference;
        this.powerDefault = powerDefault;
        this.offsetReference = offsetReference;
        this.offsetDefault = offsetDefault;
        this.levelOffsetReference = levelOffsetReference;
        this.levelOffsetDefault = levelOffsetDefault;

        this.scaleListener = new PropertyWrapper(scaleReference.resolve(), scaleDefault, -Float.MAX_VALUE, Float.MAX_VALUE);
        this.powerListener = new PropertyWrapper(powerReference.resolve(), powerDefault, -Float.MAX_VALUE, Float.MAX_VALUE);
        this.offsetListener = new PropertyWrapper(offsetReference.resolve(), offsetDefault, -Float.MAX_VALUE, Float.MAX_VALUE);
        this.levelOffsetListener = new PropertyWrapper(levelOffsetReference.resolve(), levelOffsetDefault, -Float.MAX_VALUE, Float.MAX_VALUE);
    }

    /**
     * Calculates $\text{offset} + \text{scale} \cdot (\text{level} + \text{levelOffset})^{\text{power}}$
     * using the active configuration values.
     *
     * @param level The active level of the enchantment.
     * @return The calculated polynomial float value.
     */
    @Override
    public float calculate(int level) {
        float scaleVal = this.scaleListener.getValue();
        float powerVal = this.powerListener.getValue();
        float offsetVal = this.offsetListener.getValue();
        float levelOffsetVal = this.levelOffsetListener.getValue();

        return (float) (offsetVal + (scaleVal * Math.pow(level + levelOffsetVal, powerVal)));
    }

    /**
     * Returns the {@link ConfigReference} for the scale coefficient.
     *
     * @return The scale configuration reference.
     */
    public ConfigReference scaleReference() { return this.scaleReference; }

    /**
     * Returns the default fallback for the scale coefficient.
     *
     * @return The default scale float value.
     */
    public Float scaleDefault() { return this.scaleDefault; }

    /**
     * Returns the {@link ConfigReference} for the exponent power.
     *
     * @return The power configuration reference.
     */
    public ConfigReference powerReference() { return this.powerReference; }

    /**
     * Returns the default fallback for the exponent power.
     *
     * @return The default power float value.
     */
    public Float powerDefault() { return this.powerDefault; }

    /**
     * Returns the {@link ConfigReference} for the additive output offset.
     *
     * @return The offset configuration reference.
     */
    public ConfigReference offsetReference() { return this.offsetReference; }

    /**
     * Returns the default fallback for the additive output offset.
     *
     * @return The default offset float value.
     */
    public Float offsetDefault() { return this.offsetDefault; }

    /**
     * Returns the {@link ConfigReference} for the pre-exponent level offset.
     *
     * @return The level-offset configuration reference.
     */
    public ConfigReference levelOffsetReference() { return this.levelOffsetReference; }

    /**
     * Returns the default fallback for the pre-exponent level offset.
     *
     * @return The default level-offset float value.
     */
    public Float levelOffsetDefault() { return this.levelOffsetDefault; }

    @Override
    public @NotNull MapCodec<? extends LevelBasedValue> codec() {
        return CODEC;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ConfigurablePolynomialValue) obj;
        return Objects.equals(this.scaleReference, that.scaleReference) &&
                Objects.equals(this.powerReference, that.powerReference) &&
                Objects.equals(this.offsetReference, that.offsetReference) &&
                Objects.equals(this.levelOffsetReference, that.levelOffsetReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.scaleReference, this.powerReference, this.offsetReference, this.levelOffsetReference);
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