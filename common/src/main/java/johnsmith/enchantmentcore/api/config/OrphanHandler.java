package johnsmith.enchantmentcore.api.config;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.data.ConfigDescription;

/**
 * Service abstraction for handling {@link ConfigDescription} targets that reference an unregistered
 * or missing {@link ConfigManager}.
 * <p>
 * When a datapack binds an enchantment property to a mod namespace that has not registered a
 * {@link ConfigManager}, the active {@link OrphanHandler} adopts the property into a dedicated fallback
 * category so that the configuration value remains accessible and editable at runtime.
 */
@FunctionalInterface
public interface OrphanHandler {
    /**
     * Adopts an orphaned configuration description into a designated fallback configuration space.
     *
     * @param orphan The orphaned {@link ConfigDescription} referencing an unavailable {@link ConfigManager}.
     * @return A remapped {@link ConfigDescription} pointing to a valid fallback configuration manager and category.
     */
    ConfigDescription adopt(ConfigDescription orphan);

    /**
     * Global service provider holding the active {@link OrphanHandler} implementation.
     */
    final class Provider {
        /** The active handler instance, defaulting to an identity no-op fallback prior to initialization. */
        private static OrphanHandler instance = orphan -> orphan;

        /** Private constructor to prevent instantiation. */
        private Provider() {}

        /**
         * Assigns the global {@link OrphanHandler} implementation.
         *
         * @param handler The handler implementation to install.
         */
        public static void set(OrphanHandler handler) {
            instance = handler;
        }

        /**
         * Retrieves the active global {@link OrphanHandler} implementation.
         *
         * @return The installed {@link OrphanHandler} instance.
         */
        public static OrphanHandler get() {
            return instance;
        }
    }
}