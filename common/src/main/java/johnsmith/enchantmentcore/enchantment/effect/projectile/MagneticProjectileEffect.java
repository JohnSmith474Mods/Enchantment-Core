package johnsmith.enchantmentcore.enchantment.effect.projectile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

public record MagneticProjectileEffect(
        LevelBasedValue pullStrength,
        boolean prioritizeHead,
        LevelBasedValue armingDistance,
        LevelBasedValue searchRadius
) {
    public static final String PULL_STRENGTH = "pull_strength";
    public static final String PRIORITIZE_HEAD = "prioritize_head";
    public static final String ARMING_DISTANCE = "arming_distance";
    public static final String SEARCH_RADIUS = "search_radius";

    public static final String KEY = "projectile_magnetism";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            PULL_STRENGTH, ARMING_DISTANCE, SEARCH_RADIUS
    );

    public static final Codec<MagneticProjectileEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(PULL_STRENGTH).forGetter(MagneticProjectileEffect::pullStrength),
            Codec.BOOL.optionalFieldOf(PRIORITIZE_HEAD, false).forGetter(MagneticProjectileEffect::prioritizeHead),
            LevelBasedValue.CODEC.optionalFieldOf(ARMING_DISTANCE, LevelBasedValue.constant(0.0f)).forGetter(MagneticProjectileEffect::armingDistance),
            LevelBasedValue.CODEC.optionalFieldOf(SEARCH_RADIUS, LevelBasedValue.constant(16.0f)).forGetter(MagneticProjectileEffect::searchRadius)
    ).apply(instance, MagneticProjectileEffect::new));
}