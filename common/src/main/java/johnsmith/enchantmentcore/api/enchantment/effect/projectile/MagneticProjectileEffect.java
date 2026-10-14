package johnsmith.enchantmentcore.api.enchantment.effect.projectile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Data-driven projectile enchantment effect component that curves a projectile's flight trajectory
 * directly toward the nearest living entity within a radial search area, without FOV or turn-rate constraints.
 *
 * @param pullStrength   The {@link LevelBasedValue} governing the strength of the trajectory attraction toward the target.
 * @param prioritizeHead Whether magnetic attraction targets the entity's eye position rather than its center of mass.
 * @param armingDistance The {@link LevelBasedValue} minimum distance (in blocks) the projectile must travel before magnetism activates.
 * @param searchRadius   The {@link LevelBasedValue} radius (in blocks) searched around the projectile for candidate targets.
 */
public record MagneticProjectileEffect(
        LevelBasedValue pullStrength,
        boolean prioritizeHead,
        LevelBasedValue armingDistance,
        LevelBasedValue searchRadius
) {
    /** JSON field key for the magnetic pull strength. */
    public static final String PULL_STRENGTH = "pull_strength";

    /** JSON field key for prioritizing target eye height. */
    public static final String PRIORITIZE_HEAD = "prioritize_head";

    /** JSON field key for the minimum arming distance in blocks. */
    public static final String ARMING_DISTANCE = "arming_distance";

    /** JSON field key for the radial target search distance in blocks. */
    public static final String SEARCH_RADIUS = "search_radius";

    /** Registry path identifier for this projectile effect component. */
    public static final String KEY = "projectile_magnetism";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            PULL_STRENGTH, ARMING_DISTANCE, SEARCH_RADIUS
    );

    /**
     * Codec for serializing and deserializing {@link MagneticProjectileEffect} instances from JSON.
     */
    public static final Codec<MagneticProjectileEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(PULL_STRENGTH).forGetter(MagneticProjectileEffect::pullStrength),
            Codec.BOOL.optionalFieldOf(PRIORITIZE_HEAD, false).forGetter(MagneticProjectileEffect::prioritizeHead),
            LevelBasedValue.CODEC.optionalFieldOf(ARMING_DISTANCE, LevelBasedValue.constant(0.0f)).forGetter(MagneticProjectileEffect::armingDistance),
            LevelBasedValue.CODEC.optionalFieldOf(SEARCH_RADIUS, LevelBasedValue.constant(16.0f)).forGetter(MagneticProjectileEffect::searchRadius)
    ).apply(instance, MagneticProjectileEffect::new));
}