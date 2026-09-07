package johnsmith.enchantmentcore.api.entity.accessor;

/**
 * Interface facilitating the tracking and progression of consecutive interaction events against targeted identifiers.
 */
public interface StreakStateAccessor {
    /**
     * Retrieves the current accumulated streak count for the designated target identifier.
     *
     * @param targetId The unique string identifier for the streak target.
     * @return The current streak count.
     */
    int enchantment_core$getStreakCount(String targetId);

    /**
     * Increments the current streak count for the specified target identifier and refreshes its timeout constraint.
     *
     * @param targetId     The unique string identifier for the streak target.
     * @param maxStreak    The absolute ceiling for the accumulated streak value.
     * @param timeoutTicks The maximum operational window in ticks before the streak resets.
     */
    void enchantment_core$incrementStreak(String targetId, int maxStreak, int timeoutTicks);
}