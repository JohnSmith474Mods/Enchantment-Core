package johnsmith.enchantmentcore.api.entity.accessor;

/**
 * Duck interface injected into {@link net.minecraft.world.entity.projectile.Projectile} via {@code StateDeclarationMixin}
 * defining mutable physics and behavioral parameters for in-flight projectiles.
 * <p>
 * Populated once at the start of a projectile's flight by {@code StateCaptureMixin} and queried by
 * aerodynamic drag, homing, magnetism, ricochet, and shrapnel mixins.
 */
public interface ProjectileStateAccessor extends EntityStateAccessor {
    /**
     * Sets the multiplier applied to the projectile's base aerodynamic/fluid velocity retention factor.
     *
     * @param multiplier The drag multiplier.
     */
    void enchantment_core$setDragMultiplier(double multiplier);

    /**
     * Sets the steering interpolation strength applied when tracking a homing target.
     *
     * @param strength The homing tracking strength.
     */
    void enchantment_core$setHomingStrength(double strength);

    /**
     * Sets whether homing steering aims for the target's eye height instead of its bounding box center.
     *
     * @param prioritizesHead {@code true} to prioritize the head/eyes, {@code false} for center of mass.
     */
    void enchantment_core$setHomingPrioritizesHead(boolean prioritizesHead);

    /**
     * Sets the minimum distance (in blocks) the projectile must travel from its spawn point before homing activates.
     *
     * @param distance The arming distance in blocks.
     */
    void enchantment_core$setHomingArmingDistance(double distance);

    /**
     * Sets the near-clipping plane distance (in blocks) of the homing target acquisition frustum.
     *
     * @param distance The minimum tracking distance in blocks.
     */
    void enchantment_core$setHomingMinDistance(double distance);

    /**
     * Sets the far-clipping plane distance (in blocks) of the homing target acquisition frustum.
     *
     * @param distance The maximum tracking distance in blocks.
     */
    void enchantment_core$setHomingMaxDistance(double distance);

    /**
     * Sets the field-of-view cone angle (in degrees) restricting homing target acquisition.
     *
     * @param fov The field-of-view angle in degrees.
     */
    void enchantment_core$setHomingFov(double fov);

    /**
     * Sets the maximum angular rotation (in degrees per tick) the projectile can turn during homing.
     *
     * @param turnRate The turn rate limit in degrees per tick.
     */
    void enchantment_core$setHomingTurnRate(double turnRate);

    /**
     * Sets the number of block-surface ricochet bounces remaining for the projectile.
     *
     * @param bounces The number of bounces remaining.
     */
    void enchantment_core$setBouncesRemaining(int bounces);

    /**
     * Sets the fraction of velocity magnitude retained after a ricochet bounce.
     *
     * @param retention The velocity retention multiplier.
     */
    void enchantment_core$setRicochetRetention(double retention);

    /**
     * Sets the attractive pull strength steering the projectile toward nearby magnetic targets.
     *
     * @param strength The magnetic pull strength.
     */
    void enchantment_core$setMagneticStrength(double strength);

    /**
     * Sets whether magnetic trajectory attraction aims for the target's eye height.
     *
     * @param head {@code true} to target the head/eyes, {@code false} for center of mass.
     */
    void enchantment_core$setMagneticPrioritizesHead(boolean head);

    /**
     * Sets the minimum distance (in blocks) the projectile must travel before magnetic attraction activates.
     *
     * @param distance The arming distance in blocks.
     */
    void enchantment_core$setMagneticArmingDistance(double distance);

    /**
     * Sets the radial search distance (in blocks) within which candidate targets are evaluated for magnetism.
     *
     * @param radius The search radius in blocks.
     */
    void enchantment_core$setMagneticSearchRadius(double radius);

    /**
     * Sets the number of recursive shrapnel split generations remaining upon impact.
     *
     * @param generations The number of shrapnel generations.
     */
    void enchantment_core$setShrapnelGenerations(int generations);

    /**
     * Sets the number of child shrapnel projectiles spawned per fracture event.
     *
     * @param amount The child projectile count per split.
     */
    void enchantment_core$setShrapnelAmount(int amount);

    /**
     * Sets the inaccuracy cone spread angle (in degrees) applied to spawned shrapnel projectiles.
     *
     * @param spread The spread angle in degrees.
     */
    void enchantment_core$setShrapnelSpread(double spread);

    /**
     * Sets the fraction of the parent projectile's speed inherited by spawned shrapnel projectiles.
     *
     * @param retention The velocity retention multiplier.
     */
    void enchantment_core$setShrapnelVelocityRetention(double retention);

    /**
     * Sets the fraction of the parent arrow's base damage inherited by spawned shrapnel arrows.
     *
     * @param retention The damage retention multiplier.
     */
    void enchantment_core$setShrapnelDamageRetention(double retention);

    /**
     * Sets whether impacting a block triggers shrapnel fracturing.
     *
     * @param trigger {@code true} to trigger on block impact, {@code false} otherwise.
     */
    void enchantment_core$setShrapnelTriggerBlock(boolean trigger);

    /**
     * Sets whether impacting an entity triggers shrapnel fracturing.
     *
     * @param trigger {@code true} to trigger on entity impact, {@code false} otherwise.
     */
    void enchantment_core$setShrapnelTriggerEntity(boolean trigger);

    /**
     * Sets the state lock flag indicating whether weapon enchantment modifiers have been evaluated and cached on this projectile.
     *
     * @param calculated {@code true} once initial evaluation is complete, {@code false} otherwise.
     */
    void enchantment_core$setCalculated(boolean calculated);

    /**
     * Retrieves the active aerodynamic drag multiplier.
     *
     * @return The drag multiplier (defaults to {@code 1.0}).
     */
    double enchantment_core$getDragMultiplier();

    /**
     * Retrieves the homing tracking strength.
     *
     * @return The homing strength (defaults to {@code 0.0}).
     */
    double enchantment_core$getHomingStrength();

    /**
     * Retrieves whether homing prioritizes the target's head/eye position.
     *
     * @return {@code true} if prioritizing head, {@code false} otherwise.
     */
    boolean enchantment_core$getHomingPrioritizesHead();

    /**
     * Retrieves the arming distance required before homing activates.
     *
     * @return The arming distance in blocks.
     */
    double enchantment_core$getHomingArmingDistance();

    /**
     * Retrieves the minimum frustum distance threshold for homing target acquisition.
     *
     * @return The minimum distance in blocks.
     */
    double enchantment_core$getHomingMinDistance();

    /**
     * Retrieves the maximum frustum distance threshold for homing target acquisition.
     *
     * @return The maximum distance in blocks (defaults to {@code 64.0}).
     */
    double enchantment_core$getHomingMaxDistance();

    /**
     * Retrieves the field-of-view cone angle for homing target acquisition.
     *
     * @return The FOV angle in degrees (defaults to {@code 360.0}).
     */
    double enchantment_core$getHomingFov();

    /**
     * Retrieves the maximum angular turn rate per tick for homing tracking.
     *
     * @return The turn rate in degrees per tick (defaults to {@code 360.0}).
     */
    double enchantment_core$getHomingTurnRate();

    /**
     * Retrieves the number of remaining ricochet bounces.
     *
     * @return The remaining bounce count.
     */
    int enchantment_core$getBouncesRemaining();

    /**
     * Retrieves the velocity retention multiplier applied after each ricochet bounce.
     *
     * @return The velocity retention multiplier (defaults to {@code 0.8}).
     */
    double enchantment_core$getRicochetRetention();

    /**
     * Retrieves the magnetic trajectory pull strength.
     *
     * @return The magnetic pull strength (defaults to {@code 0.0}).
     */
    double enchantment_core$getMagneticStrength();

    /**
     * Retrieves whether magnetic pull prioritizes the target's head/eye position.
     *
     * @return {@code true} if prioritizing head, {@code false} otherwise.
     */
    boolean enchantment_core$getMagneticPrioritizesHead();

    /**
     * Retrieves the arming distance required before the magnetic effect activates.
     *
     * @return The arming distance in blocks.
     */
    double enchantment_core$getMagneticArmingDistance();

    /**
     * Retrieves the radial target search radius for the magnetic effect.
     *
     * @return The search radius in blocks (defaults to {@code 16.0}).
     */
    double enchantment_core$getMagneticSearchRadius();

    /**
     * Retrieves the number of recursive shrapnel split generations remaining.
     *
     * @return The remaining generation count.
     */
    int enchantment_core$getShrapnelGenerations();

    /**
     * Retrieves the number of child shrapnel projectiles spawned per impact.
     *
     * @return The shrapnel projectile count.
     */
    int enchantment_core$getShrapnelAmount();

    /**
     * Retrieves the inaccuracy spread angle of spawned shrapnel projectiles.
     *
     * @return The spread angle in degrees.
     */
    double enchantment_core$getShrapnelSpread();

    /**
     * Retrieves the velocity retention multiplier for spawned shrapnel projectiles.
     *
     * @return The velocity retention multiplier.
     */
    double enchantment_core$getShrapnelVelocityRetention();

    /**
     * Retrieves the base damage retention multiplier for spawned shrapnel arrows.
     *
     * @return The damage retention multiplier.
     */
    double enchantment_core$getShrapnelDamageRetention();

    /**
     * Retrieves whether shrapnel generation triggers upon block impact.
     *
     * @return {@code true} if triggered on block impact, {@code false} otherwise.
     */
    boolean enchantment_core$getShrapnelTriggerBlock();

    /**
     * Retrieves whether shrapnel generation triggers upon entity impact.
     *
     * @return {@code true} if triggered on entity impact, {@code false} otherwise.
     */
    boolean enchantment_core$getShrapnelTriggerEntity();

    /**
     * Retrieves whether weapon enchantment modifiers have been evaluated and cached on this projectile.
     *
     * @return {@code true} if calculated, {@code false} otherwise.
     */
    boolean enchantment_core$isCalculated();
}