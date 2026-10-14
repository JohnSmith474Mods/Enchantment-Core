package johnsmith.enchantmentcore.api.config;

/**
 * Platform-agnostic service abstraction for querying whether a specific mod is loaded in the active
 * runtime environment.
 * <p>
 * Used primarily by {@link ConfigReference#resolve()} to evaluate conditional {@code "try_config"} and
 * {@code "else_config"} configuration targets across Fabric, Forge, and NeoForge.
 */
@FunctionalInterface
public interface ModLoadEvaluator {
    /**
     * Checks whether a mod with the specified identifier is currently loaded.
     *
     * @param modId The namespace/identifier of the mod to query.
     * @return {@code true} if the mod is loaded in the current environment, {@code false} otherwise.
     */
    boolean isLoaded(String modId);

    /**
     * Global service provider holding the platform-specific {@link ModLoadEvaluator} implementation.
     */
    final class Provider {
        /** The active evaluator instance, defaulting to {@code false} prior to platform initialization. */
        private static ModLoadEvaluator instance = modId -> false;

        /** Private constructor to prevent instantiation. */
        private Provider() {}

        /**
         * Assigns the global {@link ModLoadEvaluator} implementation for the current mod loader platform.
         *
         * @param evaluator The platform-specific evaluator implementation.
         */
        public static void set(ModLoadEvaluator evaluator) {
            instance = evaluator;
        }

        /**
         * Retrieves the active global {@link ModLoadEvaluator} implementation.
         *
         * @return The installed {@link ModLoadEvaluator} instance.
         */
        public static ModLoadEvaluator get() {
            return instance;
        }
    }
}