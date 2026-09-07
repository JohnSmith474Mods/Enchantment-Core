package johnsmith.enchantmentcore.enchantment.effect.projectile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

public record ShrapnelProjectileEffect(
        LevelBasedValue generations,
        LevelBasedValue amount,
        LevelBasedValue spreadDegrees,
        LevelBasedValue velocityRetention,
        LevelBasedValue damageRetention,
        boolean triggerOnBlock,
        boolean triggerOnEntity
) {
    public static final String GENERATIONS = "generations";
    public static final String AMOUNT = "amount";
    public static final String SPREAD_DEGREES = "spread_degrees";
    public static final String VELOCITY_RETENTION = "velocity_retention";
    public static final String DAMAGE_RETENTION = "damage_retention";
    public static final String TRIGGER_ON_BLOCK = "trigger_on_block";
    public static final String TRIGGER_ON_ENTITY = "trigger_on_entity";

    public static final String KEY = "projectile_shrapnel";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            GENERATIONS, AMOUNT, SPREAD_DEGREES, VELOCITY_RETENTION, DAMAGE_RETENTION
    );

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