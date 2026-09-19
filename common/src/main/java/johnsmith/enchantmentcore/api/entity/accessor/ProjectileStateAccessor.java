package johnsmith.enchantmentcore.api.entity.accessor;

/**
 * Interface defining a comprehensive suite of mutable physics and behavioral parameters for projectile entities.
 * Implemented via mixin to allow dynamic modification of projectile flight paths and impact logic.
 */
public interface ProjectileStateAccessor extends EntityStateAccessor {

    /**
     * Sets the multiplier applied to the projectile's base aerodynamic drag.
     *
     * @param multiplier The drag multiplier.
     */
    void enchantment_core$setDragMultiplier(double multiplier);

    /**
     * Sets the turning force applied to the projectile when tracking a target.
     *
     * @param strength The homing tracking strength.
     */
    void enchantment_core$setHomingStrength(double strength);

    /**
     * Sets whether the homing logic should aim for the target's head/eye level instead of the center of mass.
     *
     * @param prioritizesHead True to prioritize the head, false otherwise.
     */
    void enchantment_core$setHomingPrioritizesHead(boolean prioritizesHead);

    /**
     * Sets the minimum distance the projectile must travel before homing tracking activates.
     *
     * @param distance The arming distance in blocks.
     */
    void enchantment_core$setHomingArmingDistance(double distance);

    /**
     * Sets the minimum distance to a target required for homing tracking to remain active.
     *
     * @param distance The minimum tracking distance in blocks.
     */
    void enchantment_core$setHomingMinDistance(double distance);

    /**
     * Sets the maximum distance at which a target can be acquired by homing tracking.
     *
     * @param distance The maximum tracking distance in blocks.
     */
    void enchantment_core$setHomingMaxDistance(double distance);

    /**
     * Sets the field of view angle restricting target acquisition.
     *
     * @param fov The field of view in degrees.
     */
    void enchantment_core$setHomingFov(double fov);

    /**
     * Sets the maximum rotation angle the projectile can turn per tick during homing.
     *
     * @param turnRate The turn rate limit in degrees per tick.
     */
    void enchantment_core$setHomingTurnRate(double turnRate);

    /**
     * Sets the number of ricochet bounces remaining for the projectile.
     *
     * @param bounces The number of bounces.
     */
    void enchantment_core$setBouncesRemaining(int bounces);

    /**
     * Sets the percentage of velocity retained after a ricochet bounce.
     *
     * @param retention The velocity retention multiplier (0.0 to 1.0).
     */
    void enchantment_core$setRicochetRetention(double retention);

    /**
     * Sets the attractive force applied to nearby targets pulling them toward the projectile.
     *
     * @param strength The magnetic pull strength.
     */
    void enchantment_core$setMagneticStrength(double strength);

    /**
     * Sets whether the magnetic pull should center on the target's head.
     *
     * @param head True to target the head, false otherwise.
     */
    void enchantment_core$setMagneticPrioritizesHead(boolean head);

    /**
     * Sets the minimum distance the projectile must travel before the magnetic effect activates.
     *
     * @param distance The arming distance in blocks.
     */
    void enchantment_core$setMagneticArmingDistance(double distance);

    /**
     * Sets the radius within which entities will be affected by the magnetic pull.
     *
     * @param radius The search radius in blocks.
     */
    void enchantment_core$setMagneticSearchRadius(double radius);

    /**
     * Sets the number of times this projectile will split into shrapnel upon impact.
     *
     * @param generations The number of shrapnel generations.
     */
    void enchantment_core$setShrapnelGenerations(int generations);

    /**
     * Sets the base amount of shrapnel projectiles spawned per impact.
     *
     * @param amount The shrapnel count.
     */
    void enchantment_core$setShrapnelAmount(int amount);

    /**
     * Sets the cone angle dictating the spread of spawned shrapnel.
     *
     * @param spread The spread angle in degrees.
     */
    void enchantment_core$setShrapnelSpread(double spread);

    /**
     * Sets the percentage of original velocity transferred to the spawned shrapnel.
     *
     * @param retention The velocity retention multiplier.
     */
    void enchantment_core$setShrapnelVelocityRetention(double retention);

    /**
     * Sets the percentage of original damage transferred to the spawned shrapnel.
     *
     * @param retention The damage retention multiplier.
     */
    void enchantment_core$setShrapnelDamageRetention(double retention);

    /**
     * Sets whether impacting a block triggers the shrapnel generation.
     *
     * @param trigger True to trigger on block impact, false otherwise.
     */
    void enchantment_core$setShrapnelTriggerBlock(boolean trigger);

    /**
     * Sets whether impacting an entity triggers the shrapnel generation.
     *
     * @param trigger True to trigger on entity impact, false otherwise.
     */
    void enchantment_core$setShrapnelTriggerEntity(boolean trigger);

    /**
     * Sets the flag indicating whether projectile behavioral modifications have been fully calculated and applied.
     *
     * @param calculated True if calculation is complete, false otherwise.
     */
    void enchantment_core$setCalculated(boolean calculated);

    /**
     * Retrieves the active drag multiplier.
     *
     * @return The drag multiplier.
     */
    double enchantment_core$getDragMultiplier();

    /**
     * Retrieves the homing tracking strength.
     *
     * @return The homing strength.
     */
    double enchantment_core$getHomingStrength();

    /**
     * Retrieves whether homing prioritizes the target's head.
     *
     * @return True if prioritizing head, false otherwise.
     */
    boolean enchantment_core$getHomingPrioritizesHead();

    /**
     * Retrieves the arming distance required before homing activates.
     *
     * @return The arming distance in blocks.
     */
    double enchantment_core$getHomingArmingDistance();

    /**
     * Retrieves the minimum distance threshold for homing tracking.
     *
     * @return The minimum distance in blocks.
     */
    double enchantment_core$getHomingMinDistance();

    /**
     * Retrieves the maximum distance threshold for homing tracking.
     *
     * @return The maximum distance in blocks.
     */
    double enchantment_core$getHomingMaxDistance();

    /**
     * Retrieves the field of view angle for homing target acquisition.
     *
     * @return The FOV in degrees.
     */
    double enchantment_core$getHomingFov();

    /**
     * Retrieves the turn rate limit for homing tracking.
     *
     * @return The turn rate in degrees per tick.
     */
    double enchantment_core$getHomingTurnRate();

    /**
     * Retrieves the remaining ricochet bounces.
     *
     * @return The number of bounces remaining.
     */
    int enchantment_core$getBouncesRemaining();

    /**
     * Retrieves the velocity retention multiplier for ricochets.
     *
     * @return The velocity retention multiplier.
     */
    double enchantment_core$getRicochetRetention();

    /**
     * Retrieves the magnetic pull strength.
     *
     * @return The magnetic strength.
     */
    double enchantment_core$getMagneticStrength();

    /**
     * Retrieves whether magnetic pull prioritizes the target's head.
     *
     * @return True if prioritizing head, false otherwise.
     */
    boolean enchantment_core$getMagneticPrioritizesHead();

    /**
     * Retrieves the arming distance required before the magnetic effect activates.
     *
     * @return The arming distance in blocks.
     */
    double enchantment_core$getMagneticArmingDistance();

    /**
     * Retrieves the search radius for the magnetic effect.
     *
     * @return The search radius in blocks.
     */
    double enchantment_core$getMagneticSearchRadius();

    /**
     * Retrieves the number of shrapnel generations remaining.
     *
     * @return The generation count.
     */
    int enchantment_core$getShrapnelGenerations();

    /**
     * Retrieves the amount of shrapnel spawned per impact.
     *
     * @return The shrapnel amount.
     */
    int enchantment_core$getShrapnelAmount();

    /**
     * Retrieves the spread angle of spawned shrapnel.
     *
     * @return The spread angle in degrees.
     */
    double enchantment_core$getShrapnelSpread();

    /**
     * Retrieves the velocity retention multiplier for spawned shrapnel.
     *
     * @return The velocity retention multiplier.
     */
    double enchantment_core$getShrapnelVelocityRetention();

    /**
     * Retrieves the damage retention multiplier for spawned shrapnel.
     *
     * @return The damage retention multiplier.
     */
    double enchantment_core$getShrapnelDamageRetention();

    /**
     * Retrieves whether shrapnel generation triggers on block impact.
     *
     * @return True if triggering on block impact, false otherwise.
     */
    boolean enchantment_core$getShrapnelTriggerBlock();

    /**
     * Retrieves whether shrapnel generation triggers on entity impact.
     *
     * @return True if triggering on entity impact, false otherwise.
     */
    boolean enchantment_core$getShrapnelTriggerEntity();

    /**
     * Retrieves whether behavioral modifications have been fully calculated.
     *
     * @return True if calculated, false otherwise.
     */
    boolean enchantment_core$isCalculated();
}