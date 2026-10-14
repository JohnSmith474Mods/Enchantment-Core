package johnsmith.enchantmentcore.api.enchantment.spellfield.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Data-driven configuration record describing a status effect payload applied by
 * {@link johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.ApplyPotionEffect} or
 * {@link johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.SummonLingeringPotionEffect}.
 *
 * @param effect        The registry {@link Holder} of the {@link MobEffect} to apply.
 * @param durationTicks The {@link LevelBasedValue} governing the effect duration in ticks.
 * @param amplifier     The {@link LevelBasedValue} governing the 0-indexed effect amplifier (e.g., {@code 0} for Level I).
 */
public record PotionPayload(
        Holder<MobEffect> effect,
        LevelBasedValue durationTicks,
        LevelBasedValue amplifier
) {
    /** JSON field key for the mob effect identifier. */
    public static final String EFFECT = "effect";

    /** JSON field key for the duration in ticks. */
    public static final String DURATION_TICKS = "duration_ticks";

    /** JSON field key for the effect amplifier. */
    public static final String AMPLIFIER = "amplifier";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(DURATION_TICKS, AMPLIFIER);

    /**
     * Codec for serializing and deserializing {@link PotionPayload} instances from JSON.
     */
    public static final Codec<PotionPayload> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf(EFFECT).forGetter(PotionPayload::effect),
            LevelBasedValue.CODEC.fieldOf(DURATION_TICKS).forGetter(PotionPayload::durationTicks),
            LevelBasedValue.CODEC.fieldOf(AMPLIFIER).forGetter(PotionPayload::amplifier)
    ).apply(instance, PotionPayload::new));
}