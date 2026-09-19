package johnsmith.enchantmentcore.api.enchantment.effect;

import net.minecraft.core.HolderSet;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.material.Fluid;

/**
 * Defines the operational parameters for a fluid walking enchantment effect.
 */
public interface FluidWalkerDefinition {
    /**
     * Retrieves the set of fluids that the entity can walk on.
     *
     * @return The holder set containing the allowed fluids.
     */
    HolderSet<Fluid> allowedFluids();

    /**
     * Retrieves the speed retention multiplier applied to the entity while walking on allowed fluids.
     *
     * @return The level-based value calculating the speed retention.
     */
    LevelBasedValue speedRetention();
}