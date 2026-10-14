package johnsmith.enchantmentcore.api.enchantment.effect.projectile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Data-driven projectile enchantment effect component that fractures a projectile into multiple
 * sub-munition clones upon impacting a block or entity.
 * <p>
 * On block impact, spawned shrapnel reflects off the hit surface normal; on entity impact, shrapnel
 * prioritizes aiming at nearby secondary living targets before falling back to an even radial ring.
 *
 * @param generations       The {@link LevelBasedValue} number of recursive split generations permitted.
 * @param amount            The {@link LevelBasedValue} number of child projectiles spawned per fracture event.
 * @param spreadDegrees     The {@link LevelBasedValue} inaccuracy cone spread (in degrees) applied to child projectiles.
 * @param velocityRetention The {@link LevelBasedValue} multiplier applied to the parent projectile's speed for each child.
 * @param damageRetention   The {@link LevelBasedValue} multiplier applied to the parent arrow's base damage for each child.
 * @param triggerOnBlock    Whether colliding with a block triggers shrapnel splitting.
 * @param triggerOnEntity   Whether colliding with an entity triggers shrapnel splitting.
 */
public record ShrapnelProjectileEffect(
        LevelBasedValue generations,
        LevelBasedValue amount,
        LevelBasedValue spreadDegrees,
        LevelBasedValue velocityRetention,
        LevelBasedValue damageRetention,
        boolean triggerOnBlock,
        boolean triggerOnEntity
) {
    /** JSON field key for the number of recursive split generations. */
    public static final String GENERATIONS = "generations";

    /** JSON field key for the number of shrapnel projectiles spawned per split. */
    public static final String AMOUNT = "amount";

    /** JSON field key for the angular spread of spawned shrapnel in degrees. */
    public static final String SPREAD_DEGREES = "spread_degrees";

    /** JSON field key for the velocity retention multiplier per generation. */
    public static final String VELOCITY_RETENTION = "velocity_retention";

    /** JSON field key for the base damage retention multiplier per generation. */
    public static final String DAMAGE_RETENTION = "damage_retention";

    /** JSON field key for enabling shrapnel on block impact. */
    public static final String TRIGGER_ON_BLOCK = "trigger_on_block";

    /** JSON field key for enabling shrapnel on entity impact. */
    public static final String TRIGGER_ON_ENTITY = "trigger_on_entity";

    /** Registry path identifier for this projectile effect component. */
    public static final String KEY = "projectile_shrapnel";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            GENERATIONS, AMOUNT, SPREAD_DEGREES, VELOCITY_RETENTION, DAMAGE_RETENTION
    );

    /**
     * Codec for serializing and deserializing {@link ShrapnelProjectileEffect} instances from JSON.
     */
    public static final Codec<ShrapnelProjectileEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(GENERATIONS).forGetter(ShrapnelProjectileEffect::generations),
            LevelBasedValue.CODEC.fieldOf(AMOUNT).forGetter(ShrapnelProjectileEffect::amount),
            LevelBasedValue.CODEC.optionalFieldOf(SPREAD_DEGREES, LevelBasedValue.constant(15.0f)).forGetter(ShrapnelProjectileEffect::spreadDegrees),
            LevelBasedValue.CODEC.optionalFieldOf(VELOCITY_RETENTION, LevelBasedValue.constant(0.5f)).forGetter(ShrapnelProjectileEffect::velocityRetention),
            LevelBasedValue.CODEC.optionalFieldOf(DAMAGE_RETENTION, LevelBasedValue.constant(0.5f)).forGetter(ShrapnelProjectileEffect::damageRetention),
            Codec.BOOL.optionalFieldOf(TRIGGER_ON_BLOCK, true).forGetter(ShrapnelProjectileEffect::triggerOnBlock),
            Codec.BOOL.optionalFieldOf(TRIGGER_ON_ENTITY, true).forGetter(ShrapnelProjectileEffect::triggerOnEntity)
    ).apply(instance, ShrapnelProjectileEffect::new));
}