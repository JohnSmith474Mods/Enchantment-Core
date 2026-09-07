package johnsmith.enchantmentcore.enchantment.value.configurable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

import johnsmith.configoverhauled.api.listener.FloatPropertyListener;
import johnsmith.enchantmentcore.api.config.ConfigReference;

import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.LevelBasedValue;

import org.jetbrains.annotations.NotNull;

public final class ConfigurableLevelsSquaredValue extends FloatPropertyListener implements LevelBasedValue {
    public static final MapCodec<ConfigurableLevelsSquaredValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ConfigReference.CODEC.fieldOf("config").forGetter(ConfigurableLevelsSquaredValue::reference),
            Codec.FLOAT.fieldOf("default_added").forGetter(ConfigurableLevelsSquaredValue::defaultValue),
            Codec.FLOAT.optionalFieldOf("min", -Float.MAX_VALUE).forGetter(ConfigurableLevelsSquaredValue::min),
            Codec.FLOAT.optionalFieldOf("max", Float.MAX_VALUE).forGetter(ConfigurableLevelsSquaredValue::max)
    ).apply(instance, ConfigurableLevelsSquaredValue::new));

    private final ConfigReference reference;

    public ConfigurableLevelsSquaredValue(ConfigReference reference, Float defaultValue, Float min, Float max) {
        super(reference.resolve(), defaultValue, min, max);
        this.reference = reference;
    }

    @Override
    public float calculate(int level) {
        this.resolveProperty(false);
        return (float) Mth.square(level) + (Float) this.cachedProperty.get();
    }

    public ConfigReference reference() {
        return this.reference;
    }

    public Float defaultValue() {
        return this.defaultValue;
    }

    public Float min() {
        return this.lowerBound;
    }

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