package johnsmith.enchantmentcore.api.entity.accessor;

/**
 * Interface facilitating the storage and retrieval of custom fire duration states on an entity.
 */
public interface FireDurationAccessor {
    /**
     * Assigns the duration in seconds for which the entity should burn upon impact or activation.
     *
     * @param seconds The fire duration in seconds.
     */
    void enchantment_core$setFireDuration(float seconds);

    /**
     * Retrieves the stored fire duration.
     *
     * @return The fire duration in seconds.
     */
    float enchantment_core$getFireDuration();
}