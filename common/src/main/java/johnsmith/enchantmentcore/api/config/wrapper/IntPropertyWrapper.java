package johnsmith.enchantmentcore.api.config.wrapper;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigDescription;

/**
 * Binds an integer configuration {@link Property} and its associated {@link ConfigDescription} metadata
 * to a primitive integer field on an enchantment definition (such as {@code max_level}, {@code weight},
 * or {@code anvil_cost}).
 *
 * @param property    The live integer configuration {@link Property}, or {@code null} if unconfigured.
 * @param description The {@link ConfigDescription} metadata identifying the configuration property.
 */
public record IntPropertyWrapper(Property<Integer> property, ConfigDescription description) {
    /**
     * Determines whether a valid integer configuration property is bound to this wrapper.
     *
     * @return {@code true} if {@link #property()} is non-null, {@code false} otherwise.
     */
    public boolean isConfigured() {
        return this.property != null;
    }

    /**
     * Resolves the current integer value from the bound configuration property, or returns the provided
     * fallback value if the property is not configured.
     *
     * @param fallback The default integer value to return if unconfigured.
     * @return The active configured integer value, or {@code fallback}.
     */
    public int resolve(int fallback) {
        return this.isConfigured() ? this.property.get() : fallback;
    }
}