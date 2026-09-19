package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

public record AutoSmeltEffect(
        LevelBasedValue additionalToolUsage,
        boolean dropXp
) {
    public static final String ADDITIONAL_TOOL_USAGE = "additional_tool_usage";
    public static final String DROP_XP = "drop_xp";

    public static final String KEY = "auto_smelt";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(ADDITIONAL_TOOL_USAGE);

    public static final Codec<AutoSmeltEffect> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    LevelBasedValue.CODEC.optionalFieldOf(ADDITIONAL_TOOL_USAGE, LevelBasedValue.constant(0)).forGetter(AutoSmeltEffect::additionalToolUsage),
                    Codec.BOOL.optionalFieldOf(DROP_XP, true).forGetter(AutoSmeltEffect::dropXp)
            ).apply(instance, AutoSmeltEffect::new)
    );
}