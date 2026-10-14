package johnsmith.enchantmentcore.api.enchantment.spellfield.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

/**
 * Thread-safe event bus managing the registration and execution of global spell field interception hooks.
 * <p>
 * Allows external mods to cancel spell field activations globally or dynamically attenuate/nullify
 * topological multipliers for specific entity or block targets (e.g., for party/claim protection).
 */
public class SpellFieldEventDispatcher {

    /**
     * Functional listener that intercepts the initial activation of a {@link johnsmith.enchantmentcore.api.enchantment.spellfield.SpellFieldComponent}.
     */
    @FunctionalInterface
    public interface ApplyInterceptor {
        /**
         * Evaluates the spell field activation context before volume and target processing begins.
         *
         * @param level            The executing server level.
         * @param enchantmentLevel The level of the spell field enchantment.
         * @param context          The item usage context triggering the spell field.
         * @param target           The anchor entity or focal point of the spell field.
         * @param epicenter        The exact spatial origin vector of the spell field.
         * @return {@code true} to permit execution, or {@code false} to cancel the spell field application entirely.
         */
        boolean onApply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Vec3 epicenter);
    }

    /**
     * Functional listener that intercepts and modifies the topological scalar for individual entities
     * targeted within a spell field volume.
     */
    @FunctionalInterface
    public interface EntityTargetInterceptor {
        /**
         * Evaluates and modifies the effect scalar for a specific entity target.
         *
         * @param level            The executing server level.
         * @param enchantmentLevel The level of the spell field enchantment.
         * @param context          The item usage context triggering the spell field.
         * @param target           The anchor entity or focal point of the spell field.
         * @param victim           The candidate entity targeted by the spell field.
         * @param currentScalar    The active topological multiplier for the target.
         * @return The modified scalar value; returning {@code <= 0.0F} excludes the entity from all effects.
         */
        float onEntityTarget(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Entity victim, float currentScalar);
    }

    /**
     * Functional listener that intercepts and modifies the topological scalar for individual block coordinates
     * targeted within a spell field volume.
     */
    @FunctionalInterface
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
         * @return The modified scalar value; returning {@code <= 0.0F} excludes the block from all effects.
         */
        float onBlockTarget(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, BlockPos pos, float currentScalar);
    }

    /** Registered listeners for initial spell field application interception. */
    private static final List<ApplyInterceptor> APPLY_LISTENERS = new CopyOnWriteArrayList<>();

    /** Registered listeners for per-entity target scalar interception. */
    private static final List<EntityTargetInterceptor> ENTITY_TARGET_LISTENERS = new CopyOnWriteArrayList<>();

    /** Registered listeners for per-block target scalar interception. */
    private static final List<BlockTargetInterceptor> BLOCK_TARGET_LISTENERS = new CopyOnWriteArrayList<>();

    /**
     * Registers a global spell field application interceptor.
     *
     * @param interceptor The {@link ApplyInterceptor} implementation to register.
     */
    public static void registerApplyInterceptor(ApplyInterceptor interceptor) {
        APPLY_LISTENERS.add(interceptor);
    }

    /**
     * Registers a global entity target interceptor.
     *
     * @param interceptor The {@link EntityTargetInterceptor} implementation to register.
     */
    public static void registerEntityTargetInterceptor(EntityTargetInterceptor interceptor) {
        ENTITY_TARGET_LISTENERS.add(interceptor);
    }

    /**
     * Registers a global block target interceptor.
     *
     * @param interceptor The {@link BlockTargetInterceptor} implementation to register.
     */
    public static void registerBlockTargetInterceptor(BlockTargetInterceptor interceptor) {
        BLOCK_TARGET_LISTENERS.add(interceptor);
    }

    /**
     * Dispatches the initial spell field application event to all registered {@link ApplyInterceptor}s.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param target           The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @return {@code true} if all interceptors permit execution, or {@code false} if any interceptor cancels it.
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
     * Dispatches the entity target evaluation event sequentially to all registered {@link EntityTargetInterceptor}s,
     * short-circuiting if the scalar drops to or below {@code 0.0001F}.
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
     * Dispatches the block target evaluation event sequentially to all registered {@link BlockTargetInterceptor}s,
     * short-circuiting if the scalar drops to or below {@code 0.0001F}.
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