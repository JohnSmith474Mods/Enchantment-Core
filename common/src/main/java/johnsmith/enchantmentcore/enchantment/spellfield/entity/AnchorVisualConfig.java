package johnsmith.enchantmentcore.enchantment.spellfield.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

import net.minecraft.resources.Identifier;

public record AnchorVisualConfig(
        Optional<Identifier> texture,
        Optional<Identifier> modelId,
        float scale,
        int frameCount,
        int ticksPerFrame,
        int tint
) {
    public static final Codec<AnchorVisualConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("texture").forGetter(AnchorVisualConfig::texture),
            Identifier.CODEC.optionalFieldOf("model_id").forGetter(AnchorVisualConfig::modelId),
            Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(AnchorVisualConfig::scale),
            Codec.INT.optionalFieldOf("frame_count", 1).forGetter(AnchorVisualConfig::frameCount),
            Codec.INT.optionalFieldOf("ticks_per_frame", 1).forGetter(AnchorVisualConfig::ticksPerFrame),
            Codec.INT.optionalFieldOf("tint", 0xFFFFFF).forGetter(AnchorVisualConfig::tint)
    ).apply(instance, AnchorVisualConfig::new));
}