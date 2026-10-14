package johnsmith.enchantmentcore.api.registry;

import java.util.Set;

/**
 * Functional provider supplying the JSON property key names within an effect or data record that
 * hold {@link net.minecraft.world.item.enchantment.LevelBasedValue} definitions.
 * <p>
 * Used during component and spell field effect registration to inform {@link DataTransformerRegistry}
 * which nested JSON fields should be converted into dynamic configuration objects during import and export.
 */
@FunctionalInterface
public interface LevelBasedKeyProvider {
    /**
     * Retrieves the set of JSON property keys corresponding to {@link net.minecraft.world.item.enchantment.LevelBasedValue} fields.
     *
     * @return A set containing the string identifiers of the level-based value properties.
     */
    Set<String> getLevelBasedKeys();
}