package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

public record BuoyancyEffect(
        LevelBasedValue breakSpeedMultiplier
) {
    public static final String BREAK_SPEED_MULTIPLIER = "break_speed_multiplier";

    public static final String KEY = "buoyancy";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(BREAK_SPEED_MULTIPLIER);

    public static final Codec<BuoyancyEffect> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    LevelBasedValue.CODEC.optionalFieldOf(BREAK_SPEED_MULTIPLIER, LevelBasedValue.constant(1.0F)).forGetter(BuoyancyEffect::breakSpeedMultiplier)
            ).apply(instance, BuoyancyEffect::new)
    );
}