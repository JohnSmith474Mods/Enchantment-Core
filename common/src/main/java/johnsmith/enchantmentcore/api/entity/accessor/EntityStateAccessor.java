package johnsmith.enchantmentcore.api.entity.accessor;

/**
 * Duck interface injected into {@link net.minecraft.world.entity.Entity} via {@code GravityMixin}
 * to store and query dynamic per-entity physics modifiers such as gravity scaling.
 */
public interface EntityStateAccessor {
    /**
     * Assigns the multiplier applied to the entity's base gravity acceleration in {@code Entity#getGravity()}.
     *
     * @param multiplier The gravity multiplier factor (where {@code 1.0} represents unmodified vanilla gravity).
     */
    void enchantment_core$setGravityMultiplier(double multiplier);

    /**
     * Retrieves the currently active gravity multiplier for the entity.
     *
     * @return The gravity multiplier factor (defaults to {@code 1.0} if unmodified).
     */
    double enchantment_core$getGravityMultiplier();
}