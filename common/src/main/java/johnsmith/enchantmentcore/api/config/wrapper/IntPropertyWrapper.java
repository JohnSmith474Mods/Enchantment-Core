package johnsmith.enchantmentcore.api.config.wrapper;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigDescription;

/**
 * Wraps an integer configuration property and its configuration description.
 *
 * @param property    The configuration property.
 * @param description The configuration description.
 */
public record IntPropertyWrapper(Property<Integer> property, ConfigDescription description) {
    /**
     * Determines if the configuration property is present.
     *
     * @return True if the property is not null, false otherwise.
     */
    public boolean isConfigured() {
        return this.property != null;
    }

    /**
     * Resolves the integer value from the property or returns the fallback.
     *
     * @param fallback The default value to return if unconfigured.
     * @return The resolved integer value.
     */
    public int resolve(int fallback) {
        return this.isConfigured() ? this.property.get() : fallback;
    }
}