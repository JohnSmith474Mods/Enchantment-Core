package johnsmith.enchantmentcore.enchantment.effect.projectile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

public record RicochetProjectileEffect(
        LevelBasedValue maxBounces,
        LevelBasedValue velocityRetention
) {
    public static final String MAX_BOUNCES = "max_bounces";
    public static final String VELOCITY_RETENTION = "velocity_retention";

    public static final String KEY = "projectile_ricochet";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            MAX_BOUNCES, VELOCITY_RETENTION
    );

    public static final Codec<RicochetProjectileEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(MAX_BOUNCES).forGetter(RicochetProjectileEffect::maxBounces),
            LevelBasedValue.CODEC.optionalFieldOf(VELOCITY_RETENTION, LevelBasedValue.constant(0.8f)).forGetter(RicochetProjectileEffect::velocityRetention)
    ).apply(instance, RicochetProjectileEffect::new));
}