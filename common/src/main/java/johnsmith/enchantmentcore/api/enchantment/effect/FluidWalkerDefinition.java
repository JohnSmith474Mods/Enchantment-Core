package johnsmith.enchantmentcore.api.enchantment.effect;

import net.minecraft.core.HolderSet;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.material.Fluid;

/**
 * Contract defining the operational parameters for a fluid-walking enchantment effect.
 * <p>
 * Queried during block collision evaluation in {@code FluidWalkerMixin} and movement speed factor
 * calculation in {@code PassiveEffectsMixin} to allow entities to walk across fluid surfaces as if solid.
 */
public interface FluidWalkerDefinition {
    /**
     * Retrieves the set of fluids that the entity can walk across as a solid surface.
     *
     * @return The {@link HolderSet} containing the allowed {@link Fluid}s.
     */
    HolderSet<Fluid> allowedFluids();

    /**
     * Retrieves the movement speed retention multiplier applied to the entity while walking atop allowed fluids.
     *
     * @return The {@link LevelBasedValue} calculating the speed retention factor.
     */
    LevelBasedValue speedRetention();
}