package johnsmith.enchantmentcore.api.enchantment.value.configurable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.listener.FloatPropertyListener;
import johnsmith.enchantmentcore.api.config.ConfigReference;

import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.LevelBasedValue;

import org.jetbrains.annotations.NotNull;

/**
 * Dynamic configuration-backed counterpart to {@link LevelBasedValue.Clamped}.
 * <p>
 * Evaluates an inner {@link LevelBasedValue} and clamps its output between live minimum and maximum
 * float configuration properties resolved via {@link ConfigReference}.
 */
public final class ConfigurableClampedValue implements LevelBasedValue {
    /** Registry path identifier for this level based value. */
    public static final String KEY = "configurable_clamped";

    /**
     * MapCodec for serializing and deserializing {@link ConfigurableClampedValue} instances from JSON.
     */
    public static final MapCodec<ConfigurableClampedValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf("value").forGetter(ConfigurableClampedValue::value),
            ConfigReference.CODEC.fieldOf("min_config").forGetter(ConfigurableClampedValue::minReference),
            Codec.FLOAT.fieldOf("min_default").forGetter(ConfigurableClampedValue::minDefault),
            ConfigReference.CODEC.fieldOf("max_config").forGetter(ConfigurableClampedValue::maxReference),
            Codec.FLOAT.fieldOf("max_default").forGetter(ConfigurableClampedValue::maxDefault)
    ).apply(instance, ConfigurableClampedValue::new));

    /** The inner value provider whose result is clamped. */
    private final LevelBasedValue value;

    /** Configuration reference for the minimum clamp bound. */
    private final ConfigReference minReference;

    /** Default fallback value for the minimum clamp bound. */
    private final Float minDefault;

    /** Configuration reference for the maximum clamp bound. */
    private final ConfigReference maxReference;

    /** Default fallback value for the maximum clamp bound. */
    private final Float maxDefault;

    /** Live property listener for the minimum bound. */
    private final PropertyWrapper minListener;

    /** Live property listener for the maximum bound. */
    private final PropertyWrapper maxListener;

    /**
     * Constructs a {@link ConfigurableClampedValue} bound to the specified min and max configuration references.
     *
     * @param value        The inner {@link LevelBasedValue} to clamp.
     * @param minReference The {@link ConfigReference} governing the lower clamp bound.
     * @param minDefault   The default lower clamp bound.
     * @param maxReference The {@link ConfigReference} governing the upper clamp bound.
     * @param maxDefault   The default upper clamp bound.
     */
    public ConfigurableClampedValue(LevelBasedValue value, ConfigReference minReference, Float minDefault,
                                    ConfigReference maxReference, Float maxDefault) {
        this.value = value;
        this.minReference = minReference;
        this.minDefault = minDefault;
        this.maxReference = maxReference;
        this.maxDefault = maxDefault;

        this.minListener = new PropertyWrapper(minReference.resolve(), minDefault, -Float.MAX_VALUE, Float.MAX_VALUE);
        this.maxListener = new PropertyWrapper(maxReference.resolve(), maxDefault, -Float.MAX_VALUE, Float.MAX_VALUE);
    }

    /**
     * Evaluates the inner {@link LevelBasedValue} at {@code level} and clamps the result between the
     * currently configured minimum and maximum values.
     *
     * @param level The active level of the enchantment.
     * @return The clamped float value.
     */
    @Override
    public float calculate(int level) {
        return Mth.clamp(this.value.calculate(level), this.minListener.getValue(), this.maxListener.getValue());
    }

    /**
     * Returns the inner {@link LevelBasedValue} being clamped.
     *
     * @return The inner {@link LevelBasedValue}.
     */
    public LevelBasedValue value() { return this.value; }

    /**
     * Returns the {@link ConfigReference} for the minimum clamp bound.
     *
     * @return The minimum bound configuration reference.
     */
    public ConfigReference minReference() { return this.minReference; }

    /**
     * Returns the default fallback value for the minimum clamp bound.
     *
     * @return The default minimum float value.
     */
    public Float minDefault() { return this.minDefault; }

    /**
     * Returns the {@link ConfigReference} for the maximum clamp bound.
     *
     * @return The maximum bound configuration reference.
     */
    public ConfigReference maxReference() { return this.maxReference; }

    /**
     * Returns the default fallback value for the maximum clamp bound.
     *
     * @return The default maximum float value.
     */
    public Float maxDefault() { return this.maxDefault; }

    @Override
    public @NotNull MapCodec<? extends LevelBasedValue> codec() {
        return CODEC;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ConfigurableClampedValue) obj;
        return Objects.equals(this.value, that.value) &&
                Objects.equals(this.minReference, that.minReference) &&
                Objects.equals(this.maxReference, that.maxReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.minReference, this.maxReference);
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