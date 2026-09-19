package johnsmith.enchantmentcore.api.enchantment.spellfield.effect;

import java.util.List;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

/**
 * Defines an effect evaluated globally against the entire spell field geometry.
 */
public interface SpellFieldVolumeEffect extends SpellFieldEffect {

    /**
     * Executes the volume-based modification logic.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param volumes          The list of global volumes defining the spell field geometry.
     */
    void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, List<GlobalVolume> volumes);
}