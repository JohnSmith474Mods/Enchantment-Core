package johnsmith.enchantmentcore.enchantment.spellfield.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.enchantment.LevelBasedValue;

import java.util.Set;

public record PotionPayload(
        Holder<MobEffect> effect,
        LevelBasedValue durationTicks,
        LevelBasedValue amplifier
) {
    public static final String EFFECT = "effect";
    public static final String DURATION_TICKS = "duration_ticks";
    public static final String AMPLIFIER = "amplifier";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(DURATION_TICKS, AMPLIFIER);

    public static final Codec<PotionPayload> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf(EFFECT).forGetter(PotionPayload::effect),
            LevelBasedValue.CODEC.fieldOf(DURATION_TICKS).forGetter(PotionPayload::durationTicks),
            LevelBasedValue.CODEC.fieldOf(AMPLIFIER).forGetter(PotionPayload::amplifier)
    ).apply(instance, PotionPayload::new));
}