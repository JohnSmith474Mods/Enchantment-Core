package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Contract defining an outer axis-aligned bounding box ({@link AABB}) structural frame that restricts
 * spatial target collection for a {@link johnsmith.enchantmentcore.api.enchantment.spellfield.shape.SpellFieldVolume}.
 */
public interface SpellFieldShape {
    /**
     * Computes the world-space axis-aligned bounding box enclosing the shape at the given epicenter.
     *
     * @param caster           The reference entity anchoring or orienting the shape, or {@code null}.
     * @param epicenter        The world-space center coordinate of the shape evaluation.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @return The computed world-space {@link AABB} restricting field evaluation.
     */
    AABB computeBounds(Entity caster, Vec3 epicenter, int enchantmentLevel);

    /**
     * Retrieves the {@link MapCodec} responsible for serializing and deserializing this shape definition.
     *
     * @return The registered {@link MapCodec} instance.
     */
    MapCodec<? extends SpellFieldShape> codec();
}