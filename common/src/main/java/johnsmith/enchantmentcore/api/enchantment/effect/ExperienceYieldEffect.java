package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Data-driven enchantment effect component that multiplies the experience orbs dropped from breaking
 * blocks (including auto-smelted block drops) and killing living entities.
 *
 * @param multiplier The {@link LevelBasedValue} factor by which generated experience is multiplied.
 */
public record ExperienceYieldEffect(
        LevelBasedValue multiplier
) {
    /** JSON field key for the experience yield multiplier. */
    public static final String MULTIPLIER = "multiplier";

    /** Registry path identifier for this effect component. */
    public static final String KEY = "experience_yield_multiplier";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(MULTIPLIER);

    /**
     * Codec for serializing and deserializing {@link ExperienceYieldEffect} instances from JSON.
     */
    public static final Codec<ExperienceYieldEffect> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    LevelBasedValue.CODEC.fieldOf(MULTIPLIER).forGetter(ExperienceYieldEffect::multiplier)
            ).apply(instance, ExperienceYieldEffect::new)
    );
}