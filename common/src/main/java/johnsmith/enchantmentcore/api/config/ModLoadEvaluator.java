package johnsmith.enchantmentcore.api.config;

/**
 * Abstraction for evaluating mod load status across different mod loader platforms.
 */
public interface ModLoadEvaluator {
    /**
     * Checks if a mod is currently loaded in the runtime environment.
     *
     * @param modId The target mod identifier.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isLoaded(String modId);

    /**
     * Global provider for the mod load evaluator instance.
     */
    class Provider {
        private static ModLoadEvaluator instance = modId -> false;

        /**
         * Sets the global mod load evaluator instance.
         *
         * @param evaluator The evaluator implementation.
         */
        public static void set(ModLoadEvaluator evaluator) {
            instance = evaluator;
        }

        /**
         * Retrieves the global mod load evaluator instance.
         *
         * @return The evaluator implementation.
         */
        public static ModLoadEvaluator get() {
            return instance;
        }
    }
}