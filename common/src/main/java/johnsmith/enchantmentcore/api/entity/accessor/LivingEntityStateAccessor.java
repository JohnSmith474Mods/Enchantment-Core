package johnsmith.enchantmentcore.api.entity.accessor;

import java.util.List;

import johnsmith.enchantmentcore.api.enchantment.effect.FluidWalkerDefinition;

/**
 * Duck interface injected into {@link net.minecraft.world.entity.LivingEntity} via {@code PassiveEffectsMixin}
 * to expose active per-tick passive enchantment states such as fluid-walking definitions.
 */
public interface LivingEntityStateAccessor {
    /**
     * Retrieves the list of active {@link FluidWalkerDefinition} instances currently evaluated on the living entity.
     *
     * @return A list of active {@link FluidWalkerDefinition}s for the current tick.
     */
    List<FluidWalkerDefinition> enchantment_core$getActiveFluidWalkers();
}