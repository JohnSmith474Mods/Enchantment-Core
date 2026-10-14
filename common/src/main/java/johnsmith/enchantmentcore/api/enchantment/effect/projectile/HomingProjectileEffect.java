package johnsmith.enchantmentcore.api.enchantment.effect.projectile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Data-driven projectile enchantment effect component that enables fired projectiles to acquire targets
 * within a forward-facing view frustum and smoothly steer their velocity vector toward the locked target.
 *
 * @param trackingStrength The {@link LevelBasedValue} governing how aggressively the projectile turns toward the target angle.
 * @param prioritizeHead   Whether homing aims for the target's eye position rather than the bounding box center.
 * @param armingDistance   The {@link LevelBasedValue} minimum distance (in blocks) the projectile must travel before homing activates.
 * @param minDistance      The {@link LevelBasedValue} near-clipping plane distance for the target acquisition frustum.
 * @param maxDistance      The {@link LevelBasedValue} far-clipping plane distance for the target acquisition frustum.
 * @param fov              The {@link LevelBasedValue} field-of-view cone angle in degrees for target acquisition.
 * @param turnRate         The {@link LevelBasedValue} maximum angular rotation in degrees per tick.
 */
public record HomingProjectileEffect(
        LevelBasedValue trackingStrength,
        boolean prioritizeHead,
        LevelBasedValue armingDistance,
        LevelBasedValue minDistance,
        LevelBasedValue maxDistance,
        LevelBasedValue fov,
        LevelBasedValue turnRate
) {
    /** JSON field key for the homing steering strength. */
    public static final String TRACKING_STRENGTH = "tracking_strength";

    /** JSON field key for prioritizing target eye height. */
    public static final String PRIORITIZE_HEAD = "prioritize_head";

    /** JSON field key for the minimum arming distance in blocks. */
    public static final String ARMING_DISTANCE = "arming_distance";

    /** JSON field key for the minimum target acquisition distance. */
    public static final String MIN_DISTANCE = "min_distance";

    /** JSON field key for the maximum target acquisition distance. */
    public static final String MAX_DISTANCE = "max_distance";

    /** JSON field key for the target acquisition field-of-view cone in degrees. */
    public static final String FOV = "fov";

    /** JSON field key for the maximum turn rate in degrees per tick. */
    public static final String TURN_RATE = "turn_rate";

    /** Registry path identifier for this projectile effect component. */
    public static final String KEY = "projectile_homing";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            TRACKING_STRENGTH, ARMING_DISTANCE, MIN_DISTANCE, MAX_DISTANCE, FOV, TURN_RATE
    );

    /**
     * Codec for serializing and deserializing {@link HomingProjectileEffect} instances from JSON.
     */
    public static final Codec<HomingProjectileEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(TRACKING_STRENGTH).forGetter(HomingProjectileEffect::trackingStrength),
            Codec.BOOL.optionalFieldOf(PRIORITIZE_HEAD, false).forGetter(HomingProjectileEffect::prioritizeHead),
            LevelBasedValue.CODEC.optionalFieldOf(ARMING_DISTANCE, LevelBasedValue.constant(0.0f)).forGetter(HomingProjectileEffect::armingDistance),
            LevelBasedValue.CODEC.optionalFieldOf(MIN_DISTANCE, LevelBasedValue.constant(0.1f)).forGetter(HomingProjectileEffect::minDistance),
            LevelBasedValue.CODEC.optionalFieldOf(MAX_DISTANCE, LevelBasedValue.constant(64.0f)).forGetter(HomingProjectileEffect::maxDistance),
            LevelBasedValue.CODEC.optionalFieldOf(FOV, LevelBasedValue.constant(360.0f)).forGetter(HomingProjectileEffect::fov),
            LevelBasedValue.CODEC.optionalFieldOf(TURN_RATE, LevelBasedValue.constant(10.0f)).forGetter(HomingProjectileEffect::turnRate)
    ).apply(instance, HomingProjectileEffect::new));
}