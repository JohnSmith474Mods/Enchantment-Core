package johnsmith.enchantmentcore.api.enchantment.spellfield.effect;

import java.util.Optional;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldAxis;

/**
 * Mixin-style capability interface implemented by {@link SpellFieldEffect}s that exert directional
 * forces along a {@link FieldAxis} (such as {@link johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.ImpulseEffect}
 * and {@link johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.OrbitEffect}).
 * <p>
 * Collected during {@link johnsmith.enchantmentcore.api.enchantment.spellfield.SpellFieldComponent#apply}
 * so that {@link johnsmith.enchantmentcore.api.enchantment.spellfield.effect.visual.VectorFieldVisualEffect}
 * and the client-side {@link johnsmith.enchantmentcore.client.debug.SpellFieldDebugRenderer} can automatically
 * align particle velocities and debug vector arrows with the active kinetic field.
 */
public interface DirectionalSpellFieldEffect {
    /**
     * Retrieves the active {@link FieldAxis} along which this effect operates.
     *
     * @return An {@link Optional} containing the configured {@link FieldAxis}, or empty if no directional axis is active.
     */
    Optional<FieldAxis> getActiveAxis();
}