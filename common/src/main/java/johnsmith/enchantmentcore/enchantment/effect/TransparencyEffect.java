package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.world.item.enchantment.LevelBasedValue;

import java.util.Optional;
import java.util.Set;

public record TransparencyEffect(
        LevelBasedValue alphaMultiplier,
        Optional<LevelBasedValue> detectionMitigation
) {
    public static final String ALPHA_MULTIPLIER = "alpha_multiplier";
    public static final String DETECTION_MITIGATION = "detection_mitigation";

    public static final String KEY = "transparency";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(ALPHA_MULTIPLIER);

    public static final Codec<TransparencyEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.optionalFieldOf(ALPHA_MULTIPLIER, LevelBasedValue.constant(0.0F)).forGetter(TransparencyEffect::alphaMultiplier),
            LevelBasedValue.CODEC.optionalFieldOf(DETECTION_MITIGATION).forGetter(TransparencyEffect::detectionMitigation)
    ).apply(instance, TransparencyEffect::new));
}