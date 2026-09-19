package johnsmith.enchantmentcore.enchantment.value.configurable;

import java.util.Map;
import java.util.Objects;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;
import johnsmith.enchantmentcore.Constants;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.enchantment.LevelBasedValue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A dynamic implementation of {@link LevelBasedValue} that delegates calculation
 * logic based on the current runtime value of a mod configuration property.
 */
public class ConfigAwareValue implements LevelBasedValue {
    public static final MapCodec<ConfigAwareValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ConfigDescription.CODEC.fieldOf("config").forGetter(val -> val.target),
            Codec.unboundedMap(Codec.STRING, LevelBasedValue.CODEC).fieldOf("cases").forGetter(val -> val.cases),
            LevelBasedValue.CODEC.fieldOf("fallback").forGetter(val -> val.fallback)
    ).apply(instance, ConfigAwareValue::new));

    private final ConfigDescription target;
    private final Map<String, LevelBasedValue> cases;
    private final LevelBasedValue fallback;

    @Nullable
    private final Property<?> targetProperty;

    /**
     * Cached delegate updated via property listener to eliminate per-tick map lookups.
     * Marked volatile to ensure cross-thread visibility during concurrent calculation requests.
     */
    private volatile LevelBasedValue activeDelegate;

    public ConfigAwareValue(ConfigDescription target, Map<String, LevelBasedValue> cases, LevelBasedValue fallback) {
        this.target = target;
        this.cases = cases;
        this.fallback = fallback;

        ConfigManager manager = ConfigRegistry.getManager(target.modId());
        Property<?> resolvedProperty = null;

        if (manager != null) {
            resolvedProperty = manager.findProperty(target);

            if (resolvedProperty == null) {
                manager.logError("ConfigAwareValue: Configuration property '{}' not found. Falling back to default.", target);
            }
        } else {
            Constants.LOG.error("ConfigAwareValue: ConfigManager for mod id '{}' not found. Failed to resolve property '{}'. Falling back to default.", target.modId(), target);
        }

        this.targetProperty = resolvedProperty;

        if (this.targetProperty != null) {
            this.updateDelegate();
            this.targetProperty.addListener(new Property.Listener() {
                @Override
                public void onPropertyInvalidated() {
                    activeDelegate = fallback;
                }

                @Override
                public void onPropertyChanged() {
                    updateDelegate();
                }
            });
        } else {
            this.activeDelegate = this.fallback;
        }
    }

    private void updateDelegate() {
        if (this.targetProperty != null) {
            String currentKey = String.valueOf(this.targetProperty.get());
            this.activeDelegate = this.cases.getOrDefault(currentKey, this.fallback);
        }
    }

    @Override
    public float calculate(int level) {
        return this.activeDelegate.calculate(level);
    }

    @Override
    public @NotNull MapCodec<? extends LevelBasedValue> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "ConfigAwareValue[target=" + target + ", cases=" + cases + ", fallback=" + fallback + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigAwareValue that = (ConfigAwareValue) o;
        return Objects.equals(target, that.target) &&
                Objects.equals(cases, that.cases) &&
                Objects.equals(fallback, that.fallback);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, cases, fallback);
    }
}