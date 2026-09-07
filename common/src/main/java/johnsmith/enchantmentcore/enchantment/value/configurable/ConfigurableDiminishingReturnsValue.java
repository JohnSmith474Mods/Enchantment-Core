package johnsmith.enchantmentcore.enchantment.value.configurable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.listener.FloatPropertyListener;
import johnsmith.enchantmentcore.api.config.ConfigReference;

import net.minecraft.world.item.enchantment.LevelBasedValue;

import org.jetbrains.annotations.NotNull;

public final class ConfigurableDiminishingReturnsValue implements LevelBasedValue {
    public static final MapCodec<ConfigurableDiminishingReturnsValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ConfigReference.CODEC.fieldOf("base_config").forGetter(ConfigurableDiminishingReturnsValue::baseReference),
            Codec.FLOAT.fieldOf("base_default").forGetter(ConfigurableDiminishingReturnsValue::baseDefault),
            ConfigReference.CODEC.fieldOf("decrement_config").forGetter(ConfigurableDiminishingReturnsValue::decrementReference),
            Codec.FLOAT.fieldOf("decrement_default").forGetter(ConfigurableDiminishingReturnsValue::decrementDefault),
            ConfigReference.CODEC.fieldOf("minimum_config").forGetter(ConfigurableDiminishingReturnsValue::minimumReference),
            Codec.FLOAT.fieldOf("minimum_default").forGetter(ConfigurableDiminishingReturnsValue::minimumDefault)
    ).apply(instance, ConfigurableDiminishingReturnsValue::new));

    private final ConfigReference baseReference;
    private final Float baseDefault;

    private final ConfigReference decrementReference;
    private final Float decrementDefault;

    private final ConfigReference minimumReference;
    private final Float minimumDefault;

    private final PropertyWrapper baseListener;
    private final PropertyWrapper decrementListener;
    private final PropertyWrapper minimumListener;

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

    private float calculateIterative(int level, float baseVal, float decrementVal, float minimumVal) {
        float currentBase = baseVal;
        float total = 0;
        for (int i = 0; i < level; i++) {
            total += currentBase;
            currentBase = Math.max(currentBase - decrementVal, minimumVal);
        }
        return total;
    }

    public ConfigReference baseReference() { return this.baseReference; }
    public Float baseDefault() { return this.baseDefault; }

    public ConfigReference decrementReference() { return this.decrementReference; }
    public Float decrementDefault() { return this.decrementDefault; }

    public ConfigReference minimumReference() { return this.minimumReference; }
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