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

public final class ConfigurablePolynomialValue implements LevelBasedValue {
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

    private final ConfigReference scaleReference;
    private final Float scaleDefault;

    private final ConfigReference powerReference;
    private final Float powerDefault;

    private final ConfigReference offsetReference;
    private final Float offsetDefault;

    private final ConfigReference levelOffsetReference;
    private final Float levelOffsetDefault;

    private final PropertyWrapper scaleListener;
    private final PropertyWrapper powerListener;
    private final PropertyWrapper offsetListener;
    private final PropertyWrapper levelOffsetListener;

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

    @Override
    public float calculate(int level) {
        float scaleVal = this.scaleListener.getValue();
        float powerVal = this.powerListener.getValue();
        float offsetVal = this.offsetListener.getValue();
        float levelOffsetVal = this.levelOffsetListener.getValue();

        return (float) (offsetVal + (scaleVal * Math.pow(level + levelOffsetVal, powerVal)));
    }

    public ConfigReference scaleReference() { return this.scaleReference; }
    public Float scaleDefault() { return this.scaleDefault; }

    public ConfigReference powerReference() { return this.powerReference; }
    public Float powerDefault() { return this.powerDefault; }

    public ConfigReference offsetReference() { return this.offsetReference; }
    public Float offsetDefault() { return this.offsetDefault; }

    public ConfigReference levelOffsetReference() { return this.levelOffsetReference; }
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