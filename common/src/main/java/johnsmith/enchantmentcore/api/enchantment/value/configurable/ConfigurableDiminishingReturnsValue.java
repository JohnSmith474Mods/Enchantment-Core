package johnsmith.enchantmentcore.api.enchantment.value.configurable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.listener.FloatPropertyListener;
import johnsmith.enchantmentcore.api.config.ConfigReference;
import johnsmith.enchantmentcore.api.enchantment.value.DiminishingReturnsValue;

import net.minecraft.world.item.enchantment.LevelBasedValue;

import org.jetbrains.annotations.NotNull;

/**
 * Dynamic configuration-backed counterpart to {@link DiminishingReturnsValue}.
 * <p>
 * Binds the {@code base}, {@code decrement}, and {@code minimum} parameters of the diminishing returns
 * progression to live float configuration properties resolved via {@link ConfigReference}.
 */
public final class ConfigurableDiminishingReturnsValue implements LevelBasedValue {
    /** Registry path identifier for this level based value. */
    public static final String KEY = "configurable_diminishing_returns";

    /**
     * MapCodec for serializing and deserializing {@link ConfigurableDiminishingReturnsValue} instances from JSON.
     */
    public static final MapCodec<ConfigurableDiminishingReturnsValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ConfigReference.CODEC.fieldOf("base_config").forGetter(ConfigurableDiminishingReturnsValue::baseReference),
            Codec.FLOAT.fieldOf("base_default").forGetter(ConfigurableDiminishingReturnsValue::baseDefault),
            ConfigReference.CODEC.fieldOf("decrement_config").forGetter(ConfigurableDiminishingReturnsValue::decrementReference),
            Codec.FLOAT.fieldOf("decrement_default").forGetter(ConfigurableDiminishingReturnsValue::decrementDefault),
            ConfigReference.CODEC.fieldOf("minimum_config").forGetter(ConfigurableDiminishingReturnsValue::minimumReference),
            Codec.FLOAT.fieldOf("minimum_default").forGetter(ConfigurableDiminishingReturnsValue::minimumDefault)
    ).apply(instance, ConfigurableDiminishingReturnsValue::new));

    /** Configuration reference for the initial level-1 base value. */
    private final ConfigReference baseReference;

    /** Default fallback for the base value. */
    private final Float baseDefault;

    /** Configuration reference for the per-level decrement amount. */
    private final ConfigReference decrementReference;

    /** Default fallback for the per-level decrement amount. */
    private final Float decrementDefault;

    /** Configuration reference for the minimum per-level floor increment. */
    private final ConfigReference minimumReference;

    /** Default fallback for the minimum per-level floor increment. */
    private final Float minimumDefault;

    /** Live property listener for the base value. */
    private final PropertyWrapper baseListener;

    /** Live property listener for the decrement value. */
    private final PropertyWrapper decrementListener;

    /** Live property listener for the minimum floor value. */
    private final PropertyWrapper minimumListener;

    /**
     * Constructs a {@link ConfigurableDiminishingReturnsValue} bound to the specified configuration references.
     *
     * @param baseReference      The {@link ConfigReference} for the initial base value.
     * @param baseDefault        The default initial base value.
     * @param decrementReference The {@link ConfigReference} for the per-level decrement.
     * @param decrementDefault   The default per-level decrement.
     * @param minimumReference   The {@link ConfigReference} for the minimum floor increment.
     * @param minimumDefault     The default minimum floor increment.
     */
    public ConfigurableDiminishingReturnsValue(ConfigReference baseReference, Float baseDefault,
                                               ConfigReference decrementReference, Float decrementDefault,
                                               ConfigReference minimumReference, Float minimumDefault) {
        this.baseReference = baseReference;
        this.baseDefault = baseDefault;
        this.decrementReference = decrementReference;
        this.decrementDefault = decrementDefault;
        this.minimumReference = minimumReference;
        this.minimumDefault = minimumDefault;

        this.baseListener = new PropertyWrapper(baseReference.resolve(), baseDefault, -Float.MAX_VALUE, Float.MAX_VALUE);
        this.decrementListener = new PropertyWrapper(decrementReference.resolve(), decrementDefault, -Float.MAX_VALUE, Float.MAX_VALUE);
        this.minimumListener = new PropertyWrapper(minimumReference.resolve(), minimumDefault, -Float.MAX_VALUE, Float.MAX_VALUE);
    }

    /**
     * Calculates the cumulative diminishing-returns value at the specified enchantment level using the
     * active configuration values for {@code base}, {@code decrement}, and {@code minimum}.
     *
     * @param level The active level of the enchantment.
     * @return The cumulative calculated float value, or {@code 0} if {@code level <= 0}.
     */
    @Override
    public float calculate(int level) {
        if (level <= 0) return 0;

        float baseVal = this.baseListener.getValue();
        float decrementVal = this.decrementListener.getValue();
        float minimumVal = this.minimumListener.getValue();

        if (decrementVal <= 0.0001f) {
            if (decrementVal == 0) {
                return level * Math.max(baseVal, minimumVal);
            }
            return calculateIterative(level, baseVal, decrementVal, minimumVal);
        }

        float limit = (baseVal - minimumVal) / decrementVal;
        int termCount = Math.max(1, (int) Math.floor(limit) + 1);
        int count = Math.min(level, termCount);

        float sum = count * (2 * baseVal - (count - 1) * decrementVal) / 2f;

        if (level > count) {
            int remaining = level - count;
            sum += remaining * minimumVal;
        }

        return sum;
    }

    /**
     * Fallback iterative summation used when the configured decrement is negative or near zero.
     *
     * @param level        The enchantment level.
     * @param baseVal      The active base value.
     * @param decrementVal The active decrement value.
     * @param minimumVal   The active minimum floor value.
     * @return The iteratively summed float value.
     */
    private float calculateIterative(int level, float baseVal, float decrementVal, float minimumVal) {
        float currentBase = baseVal;
        float total = 0;
        for (int i = 0; i < level; i++) {
            total += currentBase;
            currentBase = Math.max(currentBase - decrementVal, minimumVal);
        }
        return total;
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
     * Returns the {@link ConfigReference} for the per-level decrement value.
     *
     * @return The decrement configuration reference.
     */
    public ConfigReference decrementReference() { return this.decrementReference; }

    /**
     * Returns the default fallback for the per-level decrement value.
     *
     * @return The default decrement float value.
     */
    public Float decrementDefault() { return this.decrementDefault; }

    /**
     * Returns the {@link ConfigReference} for the minimum floor value.
     *
     * @return The minimum floor configuration reference.
     */
    public ConfigReference minimumReference() { return this.minimumReference; }

    /**
     * Returns the default fallback for the minimum floor value.
     *
     * @return The default minimum floor float value.
     */
    public Float minimumDefault() { return this.minimumDefault; }

    @Override
    public @NotNull MapCodec<? extends LevelBasedValue> codec() {
        return CODEC;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ConfigurableDiminishingReturnsValue) obj;
        return Objects.equals(this.baseReference, that.baseReference) &&
                Objects.equals(this.decrementReference, that.decrementReference) &&
                Objects.equals(this.minimumReference, that.minimumReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.baseReference, this.decrementReference, this.minimumReference);
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