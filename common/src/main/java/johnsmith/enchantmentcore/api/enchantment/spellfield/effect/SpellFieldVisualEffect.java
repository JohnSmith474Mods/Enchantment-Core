package johnsmith.enchantmentcore.api.enchantment.spellfield.effect;

import java.util.List;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldAxis;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

/**
 * Defines a visual effect broadcasted across the evaluated spell field geometry.
 */
public interface SpellFieldVisualEffect extends SpellFieldEffect {

    /**
     * Executes the visual generation logic.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param target           The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param volumes          The list of global volumes defining the spell field geometry.
     * @param activeAxis       The list of active directional axes collected from other effects.
     */
    void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Vec3 epicenter, List<GlobalVolume> volumes, List<FieldAxis> activeAxis);
}