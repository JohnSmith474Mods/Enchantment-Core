package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

public record ExperienceYieldEffect(
        LevelBasedValue multiplier
) {
    public static final String MULTIPLIER = "multiplier";

    public static final String KEY = "experience_yield_multiplier";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(MULTIPLIER);

    public static final Codec<ExperienceYieldEffect> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    LevelBasedValue.CODEC.fieldOf(MULTIPLIER).forGetter(ExperienceYieldEffect::multiplier)
            ).apply(instance, ExperienceYieldEffect::new)
    );
}