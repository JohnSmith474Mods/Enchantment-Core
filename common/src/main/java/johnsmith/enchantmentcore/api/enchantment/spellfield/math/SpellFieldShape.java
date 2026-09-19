package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import com.mojang.serialization.MapCodec;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Defines a static outer-boundary structural frame for spatial targeting restriction.
 */
public interface SpellFieldShape {
    /**
     * Generates the primary bounding box enclosing the spell field logic.
     *
     * @param caster           The origin entity anchoring the spell field.
     * @param epicenter        The spatial center coordinate of the shape evaluation.
     * @param enchantmentLevel The level of the spell field enchantment.
     * @return The computed absolute bounding box restricting field evaluation.
     */
    AABB computeBounds(Entity caster, Vec3 epicenter, int enchantmentLevel);

    /**
     * Retrieves the codec responsible for serializing and deserializing this shape definition.
     *
     * @return The map codec instance.
     */
    MapCodec<? extends SpellFieldShape> codec();
}