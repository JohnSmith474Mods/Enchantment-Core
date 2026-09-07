package johnsmith.enchantmentcore.api.entity.accessor;

import johnsmith.enchantmentcore.api.enchantment.effect.FluidWalkerDefinition;

import java.util.List;

/**
 * Interface exposing internal runtime states of a living entity concerning custom enchantment effects.
 */
public interface LivingEntityStateAccessor {
    /**
     * Retrieves the list of active fluid walker definitions currently applied to the entity.
     *
     * @return A list of active {@link FluidWalkerDefinition} instances.
     */
    List<FluidWalkerDefinition> enchantment_core$getActiveFluidWalkers();
}