package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Data-driven enchantment effect component that configures consecutive action bonuses for mining
 * identical block types ({@link #MINING_STREAK_KEY}) or striking identical entity types ({@link #DAMAGE_STREAK_KEY}).
 * <p>
 * Each consecutive interaction against the same target identifier increments the entity's streak counter
 * (via {@link johnsmith.enchantmentcore.api.entity.accessor.StreakStateAccessor}) up to {@link #maxStreak()},
 * multiplying mining speed or combat damage by {@code 1.0 + (increment * currentStreak)} until
 * {@link #timeoutTicks()} elapses or a different target type is interacted with.
 *
 * @param maxStreak    The {@link LevelBasedValue} ceiling on the accumulated streak counter.
 * @param increment    The {@link LevelBasedValue} bonus multiplier added per accumulated streak stack.
 * @param timeoutTicks The number of ticks before an inactive streak resets to zero.
 */
public record StreakEffect(
        LevelBasedValue maxStreak,
        LevelBasedValue increment,
        int timeoutTicks
) {
    /** JSON field key for the maximum streak cap. */
    public static final String MAX_STREAK = "max_streak";

    /** JSON field key for the per-stack bonus multiplier increment. */
    public static final String INCREMENT = "increment";

    /** JSON field key for the streak expiration timeout in ticks. */
    public static final String TIMEOUT_TICKS = "timeout_ticks";

    /** Registry path identifier for the block mining streak component. */
    public static final String MINING_STREAK_KEY = "mining_streak";

    /** Registry path identifier for the combat damage streak component. */
    public static final String DAMAGE_STREAK_KEY = "damage_streak";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(MAX_STREAK, INCREMENT);

    /**
     * Codec for serializing and deserializing {@link StreakEffect} instances from JSON.
     */
    public static final Codec<StreakEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(MAX_STREAK).forGetter(StreakEffect::maxStreak),
            LevelBasedValue.CODEC.fieldOf(INCREMENT).forGetter(StreakEffect::increment),
            Codec.INT.optionalFieldOf(TIMEOUT_TICKS, 100).forGetter(StreakEffect::timeoutTicks)
    ).apply(instance, StreakEffect::new));
}