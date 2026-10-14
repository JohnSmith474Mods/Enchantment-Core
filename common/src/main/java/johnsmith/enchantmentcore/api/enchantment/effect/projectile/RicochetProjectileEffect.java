package johnsmith.enchantmentcore.api.enchantment.effect.projectile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Data-driven projectile enchantment effect component that causes arrows to bounce off solid block
 * surfaces instead of embedding on impact.
 *
 * @param maxBounces        The {@link LevelBasedValue} governing the maximum number of block ricochets allowed.
 * @param velocityRetention The {@link LevelBasedValue} fraction of speed retained after each bounce (default {@code 0.8}).
 */
public record RicochetProjectileEffect(
        LevelBasedValue maxBounces,
        LevelBasedValue velocityRetention
) {
    /** JSON field key for the maximum number of bounces. */
    public static final String MAX_BOUNCES = "max_bounces";

    /** JSON field key for the post-bounce velocity retention multiplier. */
    public static final String VELOCITY_RETENTION = "velocity_retention";

    /** Registry path identifier for this projectile effect component. */
    public static final String KEY = "projectile_ricochet";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            MAX_BOUNCES, VELOCITY_RETENTION
    );

    /**
     * Codec for serializing and deserializing {@link RicochetProjectileEffect} instances from JSON.
     */
    public static final Codec<RicochetProjectileEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(MAX_BOUNCES).forGetter(RicochetProjectileEffect::maxBounces),
            LevelBasedValue.CODEC.optionalFieldOf(VELOCITY_RETENTION, LevelBasedValue.constant(0.8f)).forGetter(RicochetProjectileEffect::velocityRetention)
    ).apply(instance, RicochetProjectileEffect::new));
}