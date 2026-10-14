package johnsmith.enchantmentcore.api.enchantment.spellfield.effect;

import com.mojang.serialization.MapCodec;

/**
 * Root polymorphic interface for all data-driven spell field effects registered in
 * {@link johnsmith.enchantmentcore.api.registry.EnchantmentCoreRegistries#SPELL_FIELD_EFFECTS}.
 */
public interface SpellFieldEffect {
    /**
     * Retrieves the {@link MapCodec} responsible for serializing and deserializing this effect.
     *
     * @return The registered {@link MapCodec} instance.
     */
    MapCodec<? extends SpellFieldEffect> codec();
}