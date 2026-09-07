package johnsmith.enchantmentcore.enchantment.effect.projectile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

public record HomingProjectileEffect(
        LevelBasedValue trackingStrength,
        boolean prioritizeHead,
        LevelBasedValue armingDistance,
        LevelBasedValue minDistance,
        LevelBasedValue maxDistance,
        LevelBasedValue fov,
        LevelBasedValue turnRate
) {
    public static final String TRACKING_STRENGTH = "tracking_strength";
    public static final String PRIORITIZE_HEAD = "prioritize_head";
    public static final String ARMING_DISTANCE = "arming_distance";
    public static final String MIN_DISTANCE = "min_distance";
    public static final String MAX_DISTANCE = "max_distance";
    public static final String FOV = "fov";
    public static final String TURN_RATE = "turn_rate";

    public static final String KEY = "projectile_homing";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            TRACKING_STRENGTH, ARMING_DISTANCE, MIN_DISTANCE, MAX_DISTANCE, FOV, TURN_RATE
    );

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