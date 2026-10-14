package johnsmith.enchantmentcore.api.enchantment.spellfield.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

/**
 * Data-driven visual configuration record for a {@link SpellFieldAnchorEntity}.
 * <p>
 * Defines either a custom 3D model delegate (resolved via {@link johnsmith.enchantmentcore.api.client.render.AnchorRendererRegistry})
 * or a billboarded sprite animation rendered by {@link johnsmith.enchantmentcore.client.render.SpellFieldAnchorRenderer}.
 *
 * @param texture       Optional {@link ResourceLocation} pointing to a vertical sprite-sheet texture for billboard rendering.
 * @param modelId       Optional {@link ResourceLocation} identifying a registered {@link johnsmith.enchantmentcore.api.client.render.AnchorModelRenderer}.
 * @param scale         The visual scale multiplier applied to the rendered quad and culling bounding box.
 * @param frameCount    The total number of vertical animation frames in the sprite sheet (minimum {@code 1}).
 * @param ticksPerFrame The number of client ticks each animation frame is displayed before advancing (minimum {@code 1}).
 * @param tint          The 24-bit RGB hexadecimal color tint applied to the rendered sprite (defaults to {@code 0xFFFFFF}).
 */
public record AnchorVisualConfig(
        Optional<ResourceLocation> texture,
        Optional<ResourceLocation> modelId,
        float scale,
        int frameCount,
        int ticksPerFrame,
        int tint
) {
    /**
     * Codec for serializing and deserializing {@link AnchorVisualConfig} instances from JSON or NBT.
     */
    public static final Codec<AnchorVisualConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(AnchorVisualConfig::texture),
            ResourceLocation.CODEC.optionalFieldOf("model_id").forGetter(AnchorVisualConfig::modelId),
            Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(AnchorVisualConfig::scale),
            Codec.INT.optionalFieldOf("frame_count", 1).forGetter(AnchorVisualConfig::frameCount),
            Codec.INT.optionalFieldOf("ticks_per_frame", 1).forGetter(AnchorVisualConfig::ticksPerFrame),
            Codec.INT.optionalFieldOf("tint", 0xFFFFFF).forGetter(AnchorVisualConfig::tint)
    ).apply(instance, AnchorVisualConfig::new));
}