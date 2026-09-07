package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

public record StreakEffect(
        LevelBasedValue maxStreak,
        LevelBasedValue increment,
        int timeoutTicks
) {
    public static final String MAX_STREAK = "max_streak";
    public static final String INCREMENT = "increment";
    public static final String TIMEOUT_TICKS = "timeout_ticks";

    public static final String MINING_STREAK_KEY = "mining_streak";
    public static final String DAMAGE_STREAK_KEY = "damage_streak";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(MAX_STREAK);

    public static final Codec<StreakEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(MAX_STREAK).forGetter(StreakEffect::maxStreak),
            LevelBasedValue.CODEC.fieldOf(INCREMENT).forGetter(StreakEffect::increment),
            Codec.INT.optionalFieldOf(TIMEOUT_TICKS, 100).forGetter(StreakEffect::timeoutTicks)
    ).apply(instance, StreakEffect::new));
}