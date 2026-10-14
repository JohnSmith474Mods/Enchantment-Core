package johnsmith.enchantmentcore.api.entity.accessor;

/**
 * Duck interface injected into {@link net.minecraft.world.entity.LivingEntity} via {@code PassiveEffectsMixin}
 * for tracking consecutive mining or combat interactions against identical target identifiers
 * (used by {@link johnsmith.enchantmentcore.api.enchantment.effect.StreakEffect}).
 */
public interface StreakStateAccessor {
    /**
     * Retrieves the current accumulated streak count for the specified target registry identifier.
     *
     * @param targetId The registry key string of the target block or entity type.
     * @return The current consecutive streak count if {@code targetId} matches the active streak target, or {@code 0} otherwise.
     */
    int enchantment_core$getStreakCount(String targetId);

    /**
     * Increments the streak counter for {@code targetId} (clamped to {@code maxStreak}) if it matches the
     * previous target, or resets the counter to {@code 1} for a new target, and refreshes the expiration timer.
     *
     * @param targetId     The registry key string of the target block or entity type.
     * @param maxStreak    The maximum allowed streak count ceiling.
     * @param timeoutTicks The number of ticks before the streak expires from inactivity.
     */
    void enchantment_core$incrementStreak(String targetId, int maxStreak, int timeoutTicks);
}