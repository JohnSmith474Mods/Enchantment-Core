package johnsmith.enchantmentcore.api.enchantment.spellfield.effect;

import com.mojang.serialization.MapCodec;

/**
 * Base contract for all data-driven spell field effects.
 */
public interface SpellFieldEffect {

    /**
     * Retrieves the codec responsible for serializing and deserializing this effect.
     *
     * @return The map codec instance.
     */
    MapCodec<? extends SpellFieldEffect> codec();
}