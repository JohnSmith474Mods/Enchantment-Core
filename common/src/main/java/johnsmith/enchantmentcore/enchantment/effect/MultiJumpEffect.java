package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;

import java.util.Set;

public record MultiJumpEffect(
        EnchantmentValueEffect jumps,
        boolean allowElytra
) {
    public static final String JUMPS = "jumps";
    public static final String ALLOW_ELYTRA = "allow_elytra";

    public static final String KEY = "multi_jump";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(JUMPS);

    public static final Codec<MultiJumpEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EnchantmentValueEffect.CODEC.fieldOf(JUMPS).forGetter(MultiJumpEffect::jumps),
            Codec.BOOL.optionalFieldOf(ALLOW_ELYTRA, false).forGetter(MultiJumpEffect::allowElytra)
    ).apply(instance, MultiJumpEffect::new));
}