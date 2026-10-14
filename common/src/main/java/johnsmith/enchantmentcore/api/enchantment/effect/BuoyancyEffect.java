package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Data-driven enchantment effect component that prevents an entity from passively sinking while
 * submerged in water or lava, and optionally scales block mining speed while floating in fluids.
 *
 * @param breakSpeedMultiplier The {@link LevelBasedValue} multiplier applied to the player's block breaking
 *                             speed while submerged and ungrounded.
 */
public record BuoyancyEffect(
        LevelBasedValue breakSpeedMultiplier
) {
    /** JSON field key for the submerged mining speed multiplier. */
    public static final String BREAK_SPEED_MULTIPLIER = "break_speed_multiplier";

    /** Registry path identifier for this effect component. */
    public static final String KEY = "buoyancy";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(BREAK_SPEED_MULTIPLIER);

    /**
     * Codec for serializing and deserializing {@link BuoyancyEffect} instances from JSON.
     */
    public static final Codec<BuoyancyEffect> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    LevelBasedValue.CODEC.optionalFieldOf(BREAK_SPEED_MULTIPLIER, LevelBasedValue.constant(1.0F)).forGetter(BuoyancyEffect::breakSpeedMultiplier)
            ).apply(instance, BuoyancyEffect::new)
    );
}