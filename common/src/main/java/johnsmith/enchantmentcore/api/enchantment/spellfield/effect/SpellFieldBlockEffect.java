package johnsmith.enchantmentcore.api.enchantment.spellfield.effect;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

/**
 * Contract for a {@link SpellFieldEffect} applied to individual block coordinates within an evaluated
 * spell field volume.
 */
public interface SpellFieldBlockEffect extends SpellFieldEffect {
    /**
     * Executes the block modification or interaction logic at the specified block position.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param pos              The target block coordinate being evaluated.
     * @param scalar           The active topological multiplier at {@code pos} in $(0.0, 1.0]$.
     * @param modifiedBlocks   A mutable set tracking block coordinates already mutated during the current evaluation cycle.
     */
    void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, BlockPos pos, float scalar, Set<BlockPos> modifiedBlocks);

    /**
     * Returns the execution priority of this block effect for sorting prior to block iteration.
     * Higher priority values execute earlier in the evaluation cycle.
     *
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @return The priority scalar (defaults to {@code 0.0F}).
     */
    default float getPriority(int enchantmentLevel) {
        return 0.0F;
    }
}