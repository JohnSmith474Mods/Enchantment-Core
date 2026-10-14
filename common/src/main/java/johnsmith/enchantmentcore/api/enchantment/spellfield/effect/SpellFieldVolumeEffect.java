package johnsmith.enchantmentcore.api.enchantment.spellfield.effect;

import java.util.List;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

/**
 * Contract for a {@link SpellFieldEffect} evaluated once globally across the composite list of
 * {@link GlobalVolume}s rather than per-entity or per-block.
 */
public interface SpellFieldVolumeEffect extends SpellFieldEffect {
    /**
     * Executes the volume-wide effect logic.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param volumes          The list of evaluated world-space {@link GlobalVolume}s defining the spell field geometry.
     */
    void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, List<GlobalVolume> volumes);
}