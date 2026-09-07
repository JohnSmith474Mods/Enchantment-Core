package johnsmith.enchantmentcore.enchantment.spellfield.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

public record AnchorVisualConfig(
        Optional<ResourceLocation> texture,
        Optional<ResourceLocation> modelId,
        float scale,
        int frameCount,
        int ticksPerFrame,
        int tint
) {
    public static final Codec<AnchorVisualConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(AnchorVisualConfig::texture),
            ResourceLocation.CODEC.optionalFieldOf("model_id").forGetter(AnchorVisualConfig::modelId),
            Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(AnchorVisualConfig::scale),
            Codec.INT.optionalFieldOf("frame_count", 1).forGetter(AnchorVisualConfig::frameCount),
            Codec.INT.optionalFieldOf("ticks_per_frame", 1).forGetter(AnchorVisualConfig::ticksPerFrame),
            Codec.INT.optionalFieldOf("tint", 0xFFFFFF).forGetter(AnchorVisualConfig::tint)
    ).apply(instance, AnchorVisualConfig::new));
}