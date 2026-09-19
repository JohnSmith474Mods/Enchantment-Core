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

public final class ConfigurableLinearValue implements LevelBasedValue {
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

    private final ConfigReference baseReference;
    private final Float baseDefault;
    private final Float baseMin;
    private final Float baseMax;

    private final ConfigReference perLevelReference;
    private final Float perLevelDefault;
    private final Float perLevelMin;
    private final Float perLevelMax;

    private final PropertyWrapper baseListener;
    private final PropertyWrapper perLevelListener;

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

    @Override
    public float calculate(int level) {
        return this.baseListener.getValue() + this.perLevelListener.getValue() * (float)(level - 1);
    }

    public ConfigReference baseReference() { return this.baseReference; }
    public Float baseDefault() { return this.baseDefault; }
    public Float baseMin() { return this.baseMin; }
    public Float baseMax() { return this.baseMax; }

    public ConfigReference perLevelReference() { return this.perLevelReference; }
    public Float perLevelDefault() { return this.perLevelDefault; }
    public Float perLevelMin() { return this.perLevelMin; }
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