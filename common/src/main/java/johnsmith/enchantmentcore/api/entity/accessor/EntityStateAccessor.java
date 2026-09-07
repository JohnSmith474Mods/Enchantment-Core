package johnsmith.enchantmentcore.api.entity.accessor;

/**
 * Interface defining mutable state parameters for base entities.
 * Facilitates dynamic modification of core entity physics via mixin injection.
 */
public interface EntityStateAccessor {

    /**
     * Assigns the multiplier applied to the entity's base gravity calculation.
     *
     * @param multiplier The gravity multiplier factor.
     */
    void enchantment_core$setGravityMultiplier(double multiplier);

    /**
     * Retrieves the currently active gravity multiplier for the entity.
     *
     * @return The gravity multiplier factor.
     */
    double enchantment_core$getGravityMultiplier();
}