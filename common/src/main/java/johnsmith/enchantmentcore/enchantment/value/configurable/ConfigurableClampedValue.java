package johnsmith.enchantmentcore.enchantment.value.configurable;

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

public final class ConfigurableClampedValue implements LevelBasedValue {
    public static final MapCodec<ConfigurableClampedValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf("value").forGetter(ConfigurableClampedValue::value),
            ConfigReference.CODEC.fieldOf("min_config").forGetter(ConfigurableClampedValue::minReference),
            Codec.FLOAT.fieldOf("min_default").forGetter(ConfigurableClampedValue::minDefault),
            ConfigReference.CODEC.fieldOf("max_config").forGetter(ConfigurableClampedValue::maxReference),
            Codec.FLOAT.fieldOf("max_default").forGetter(ConfigurableClampedValue::maxDefault)
    ).apply(instance, ConfigurableClampedValue::new));

    private final LevelBasedValue value;

    private final ConfigReference minReference;
    private final Float minDefault;

    private final ConfigReference maxReference;
    private final Float maxDefault;

    private final PropertyWrapper minListener;
    private final PropertyWrapper maxListener;

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

    @Override
    public float calculate(int level) {
        return Mth.clamp(this.value.calculate(level), this.minListener.getValue(), this.maxListener.getValue());
    }

    public LevelBasedValue value() { return this.value; }

    public ConfigReference minReference() { return this.minReference; }
    public Float minDefault() { return this.minDefault; }

    public ConfigReference maxReference() { return this.maxReference; }
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
     * Internal wrapper bridging the protected resolution logic of FloatPropertyListener
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