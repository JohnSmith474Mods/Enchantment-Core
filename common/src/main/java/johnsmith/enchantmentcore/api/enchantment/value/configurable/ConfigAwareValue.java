package johnsmith.enchantmentcore.api.enchantment.value.configurable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Objects;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;
import johnsmith.enchantmentcore.Constants;

import net.minecraft.world.item.enchantment.LevelBasedValue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A dynamic switch-like implementation of {@link LevelBasedValue} that selects its active delegate
 * {@link LevelBasedValue} based on the current stringified value of a target configuration {@link Property}.
 * <p>
 * Registers a {@link Property.Listener} on the resolved configuration property so that the active
 * delegate is cached in a {@code volatile} reference and only re-evaluated when the configuration changes,
 * avoiding map lookups during per-tick enchantment calculations.
 */
public class ConfigAwareValue implements LevelBasedValue {
    /** Registry path identifier for this level based value. */
    public static final String KEY = "config_aware";
    /**
     * MapCodec for serializing and deserializing {@link ConfigAwareValue} instances from JSON.
     */
    public static final MapCodec<ConfigAwareValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ConfigDescription.CODEC.fieldOf("config").forGetter(val -> val.target),
            Codec.unboundedMap(Codec.STRING, LevelBasedValue.CODEC).fieldOf("cases").forGetter(val -> val.cases),
            LevelBasedValue.CODEC.fieldOf("fallback").forGetter(val -> val.fallback)
    ).apply(instance, ConfigAwareValue::new));

    /** The configuration property description being observed. */
    private final ConfigDescription target;

    /** Map of stringified property values to their corresponding delegate {@link LevelBasedValue}s. */
    private final Map<String, LevelBasedValue> cases;

    /** Default {@link LevelBasedValue} used if the property is missing or its current value has no matching case. */
    private final LevelBasedValue fallback;

    /** The resolved live configuration property, or {@code null} if unavailable. */
    @Nullable
    private final Property<?> targetProperty;

    /**
     * Cached delegate updated via property listener to eliminate per-tick map lookups.
     * Marked volatile to ensure cross-thread visibility during concurrent calculation requests.
     */
    private volatile LevelBasedValue activeDelegate;

    /**
     * Constructs a {@link ConfigAwareValue} and attaches a change listener to the resolved configuration property.
     *
     * @param target   The {@link ConfigDescription} identifying the configuration property to monitor.
     * @param cases    A map matching stringified configuration values to their respective {@link LevelBasedValue} delegates.
     * @param fallback The fallback {@link LevelBasedValue} used when no case matches or the property cannot be resolved.
     */
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

    /**
     * Refreshes {@link #activeDelegate} by looking up the current string representation of {@link #targetProperty}
     * in {@link #cases}, defaulting to {@link #fallback}.
     */
    private void updateDelegate() {
        if (this.targetProperty != null) {
            String currentKey = String.valueOf(this.targetProperty.get());
            this.activeDelegate = this.cases.getOrDefault(currentKey, this.fallback);
        }
    }

    /**
     * Calculates the level-based value by delegating to the currently active {@link LevelBasedValue} branch.
     *
     * @param level The active level of the enchantment.
     * @return The calculated float value from the active delegate.
     */
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