package johnsmith.enchantmentcore.api.enchantment.spellfield.effect;

import java.util.Optional;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldAxis;

/**
 * Defines an effect that operates along a specific spatial axis.
 * Broadcasts directional impulses to the debug renderer and vector-dependent systems.
 */
public interface DirectionalSpellFieldEffect {

    /**
     * Retrieves the active field axis for this effect.
     *
     * @return An optional containing the configured field axis, or empty if no axis is defined.
     */
    Optional<FieldAxis> getActiveAxis();
}