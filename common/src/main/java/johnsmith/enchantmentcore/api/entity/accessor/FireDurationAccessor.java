package johnsmith.enchantmentcore.api.entity.accessor;

/**
 * Duck interface facilitating the storage and retrieval of custom burn durations (in seconds) on an entity,
 * used by {@link johnsmith.enchantmentcore.api.enchantment.effect.SetFireDurationEffect}.
 */
public interface FireDurationAccessor {
    /**
     * Assigns the duration in seconds for which the entity should burn upon impact or ignition.
     *
     * @param seconds The fire duration in seconds.
     */
    void enchantment_core$setFireDuration(float seconds);

    /**
     * Retrieves the stored custom fire duration in seconds.
     *
     * @return The fire duration in seconds.
     */
    float enchantment_core$getFireDuration();
}