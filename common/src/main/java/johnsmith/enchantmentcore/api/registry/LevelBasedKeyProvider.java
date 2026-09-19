package johnsmith.enchantmentcore.api.registry;

import java.util.Set;

/**
 * Defines a provider for JSON keys associated with level-based properties.
 * Instructs the data transformation registry on which specific fields require conversion
 * into dynamic configuration objects during the import and export processes.
 */
@FunctionalInterface
public interface LevelBasedKeyProvider {

    /**
     * Retrieves the defined JSON keys corresponding to level-based values.
     *
     * @return A set containing the string identifiers of the level-based properties.
     */
    Set<String> getLevelBasedKeys();
}