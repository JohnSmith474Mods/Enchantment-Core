package johnsmith.enchantmentcore.api.enchantment.spellfield.effect;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

/**
 * Defines an effect applied to individual block coordinates within a spell field volume.
 */
public interface SpellFieldBlockEffect extends SpellFieldEffect {

    /**
     * Executes the block modification logic at the specified coordinate.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param pos              The target block coordinate.
     * @param scalar           The active topological multiplier for the target.
     * @param modifiedBlocks   A mutable set tracking all block coordinates modified during this execution cycle.
     */
    void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, BlockPos pos, float scalar, Set<BlockPos> modifiedBlocks);

    /**
     * Specifies the execution priority of this block effect.
     * Higher values execute earlier in the evaluation cycle.
     *
     * @param enchantmentLevel The level of the spell field enchantment.
     * @return The priority scalar.
     */
    default float getPriority(int enchantmentLevel) {
        return 0.0F;
    }
}