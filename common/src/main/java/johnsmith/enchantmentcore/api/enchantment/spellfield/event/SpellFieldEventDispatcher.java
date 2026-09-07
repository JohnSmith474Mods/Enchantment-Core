package johnsmith.enchantmentcore.api.enchantment.spellfield.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

/**
 * Manages the registration and execution of global spell field interception hooks.
 * Dispatches events during the application phases of a spell field.
 */
public class SpellFieldEventDispatcher {

    /**
     * Intercepts the initial application of a spell field.
     */
    public interface ApplyInterceptor {
        /**
         * Evaluates the application context before volume processing begins.
         *
         * @param level            The executing server level.
         * @param enchantmentLevel The level of the spell field enchantment.
         * @param context          The item usage context triggering the spell field.
         * @param target           The anchor entity or focal point of the spell field.
         * @param epicenter        The exact spatial origin vector of the spell field.
         * @return True to permit execution. False to cancel the spell field application entirely.
         */
        boolean onApply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Vec3 epicenter);
    }

    /**
     * Intercepts the targeting evaluation for individual entities within a spell field volume.
     */
    public interface EntityTargetInterceptor {
        /**
         * Evaluates and modifies the effect scalar for a specific entity target.
         *
         * @param level            The executing server level.
         * @param enchantmentLevel The level of the spell field enchantment.
         * @param context          The item usage context triggering the spell field.
         * @param target           The anchor entity or focal point of the spell field.
         * @param victim           The entity targeted by the spell field.
         * @param currentScalar    The active topological multiplier for the target.
         * @return The modified scalar value. Return 0.0F to exclude the entity from effects.
         */
        float onEntityTarget(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Entity victim, float currentScalar);
    }

    /**
     * Intercepts the targeting evaluation for individual block coordinates within a spell field volume.
     */
    public interface BlockTargetInterceptor {
        /**
         * Evaluates and modifies the effect scalar for a specific block coordinate.
         *
         * @param level            The executing server level.
         * @param enchantmentLevel The level of the spell field enchantment.
         * @param context          The item usage context triggering the spell field.
         * @param target           The anchor entity or focal point of the spell field.
         * @param pos              The block coordinate targeted by the spell field.
         * @param currentScalar    The active topological multiplier for the target.
         * @return The modified scalar value. Return 0.0F to exclude the block from effects.
         */
        float onBlockTarget(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, BlockPos pos, float currentScalar);
    }

    private static final List<ApplyInterceptor> APPLY_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<EntityTargetInterceptor> ENTITY_TARGET_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<BlockTargetInterceptor> BLOCK_TARGET_LISTENERS = new CopyOnWriteArrayList<>();

    /**
     * Registers a global apply interceptor.
     *
     * @param interceptor The interceptor implementation to register.
     */
    public static void registerApplyInterceptor(ApplyInterceptor interceptor) {
        APPLY_LISTENERS.add(interceptor);
    }

    /**
     * Registers a global entity target interceptor.
     *
     * @param interceptor The interceptor implementation to register.
     */
    public static void registerEntityTargetInterceptor(EntityTargetInterceptor interceptor) {
        ENTITY_TARGET_LISTENERS.add(interceptor);
    }

    /**
     * Registers a global block target interceptor.
     *
     * @param interceptor The interceptor implementation to register.
     */
    public static void registerBlockTargetInterceptor(BlockTargetInterceptor interceptor) {
        BLOCK_TARGET_LISTENERS.add(interceptor);
    }

    /**
     * Dispatches the apply event to all registered interceptors.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param target           The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @return True if all interceptors permit execution. False if any interceptor cancels execution.
     */
    public static boolean dispatchApply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Vec3 epicenter) {
        for (ApplyInterceptor listener : APPLY_LISTENERS) {
            if (!listener.onApply(level, enchantmentLevel, context, target, epicenter)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Dispatches the entity target event to all registered interceptors sequentially.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param target           The anchor entity or focal point of the spell field.
     * @param victim           The entity targeted by the spell field.
     * @param scalar           The initial topological multiplier.
     * @return The final modified scalar value after all interceptors execute.
     */
    public static float dispatchEntityTarget(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Entity victim, float scalar) {
        float modifiedScalar = scalar;
        for (EntityTargetInterceptor listener : ENTITY_TARGET_LISTENERS) {
            modifiedScalar = listener.onEntityTarget(level, enchantmentLevel, context, target, victim, modifiedScalar);
            if (modifiedScalar <= 0.0001F) {
                break;
            }
        }
        return modifiedScalar;
    }

    /**
     * Dispatches the block target event to all registered interceptors sequentially.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param target           The anchor entity or focal point of the spell field.
     * @param pos              The block coordinate targeted by the spell field.
     * @param scalar           The initial topological multiplier.
     * @return The final modified scalar value after all interceptors execute.
     */
    public static float dispatchBlockTarget(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, BlockPos pos, float scalar) {
        float modifiedScalar = scalar;
        for (BlockTargetInterceptor listener : BLOCK_TARGET_LISTENERS) {
            modifiedScalar = listener.onBlockTarget(level, enchantmentLevel, context, target, pos, modifiedScalar);
            if (modifiedScalar <= 0.0001F) {
                break;
            }
        }
        return modifiedScalar;
    }
}