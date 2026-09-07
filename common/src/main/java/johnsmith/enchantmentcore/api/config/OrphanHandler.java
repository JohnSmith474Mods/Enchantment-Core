package johnsmith.enchantmentcore.api.config;

import johnsmith.configoverhauled.api.data.ConfigDescription;

/**
 * Abstraction for handling configuration descriptions that reference missing configuration managers.
 */
public interface OrphanHandler {
    /**
     * Adopts an orphaned configuration description into a designated fallback configuration space.
     *
     * @param orphan The orphaned configuration description.
     * @return The adopted configuration description.
     */
    ConfigDescription adopt(ConfigDescription orphan);

    /**
     * Global provider for the orphan handler instance.
     */
    class Provider {
        private static OrphanHandler instance = orphan -> orphan; // No-op fallback

        /**
         * Sets the global orphan handler instance.
         *
         * @param handler The handler implementation.
         */
        public static void set(OrphanHandler handler) {
            instance = handler;
        }

        /**
         * Retrieves the global orphan handler instance.
         *
         * @return The handler implementation.
         */
        public static OrphanHandler get() {
            return instance;
        }
    }
}