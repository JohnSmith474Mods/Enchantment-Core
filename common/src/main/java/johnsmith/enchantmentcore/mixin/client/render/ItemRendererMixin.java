package johnsmith.enchantmentcore.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.enchantment.effect.TransparencyEffect;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting the generalized item renderer.
 * Scales the opacity of items rendered directly in the world or in player hands based on active transparency enchantments.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    /** State tracker to hold the calculated alpha multiplier during the current render pass. */
    @Unique
    private float enchantment_core$currentItemAlpha = 1.0F;

    /**
     * Intercepts the start of the core item rendering pipeline.
     * Evaluates the item's enchantments and stores the lowest valid transparency multiplier.
     *
     * @param itemStack       The item stack being rendered.
     * @param displayContext  The rendering context.
     * @param poseStack       The active transformation matrix stack.
     * @param buffer          The buffer source.
     * @param packedLight     The light level.
     * @param packedOverlay   The overlay map.
     * @param model           The baked model geometry.
     * @param renderFoil      Whether to render the enchantment glint foil.
     * @param ci              The callback information.
     */
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void enchantment_core$captureItemAlpha(
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            BakedModel model,
            boolean renderFoil,
            CallbackInfo ci
    ) {
        this.enchantment_core$currentItemAlpha = 1.0F;
        if (itemStack.isEmpty()) return;

        ItemEnchantments enchantments = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;

        float minAlpha = 1.0F;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            List<ConditionalEffect<TransparencyEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.TRANSPARENCY.get());
            if (effects != null) {
                for (ConditionalEffect<TransparencyEffect> cond : effects) {
                    minAlpha = Math.min(minAlpha, cond.effect().alphaMultiplier().calculate(entry.getIntValue()));
                }
            }
        }
        this.enchantment_core$currentItemAlpha = Math.max(0.0F, Math.min(1.0F, minAlpha));
    }

    /**
     * Intercepts the vertex consumer passed to the baked model rendering engine.
     * Wraps the consumer to dynamically scale the alpha component of every vertex color call.
     *
     * @param originalConsumer The vanilla vertex consumer.
     * @return The wrapped vertex consumer, or the original if no transparency modification is required.
     */
    @ModifyArg(
            method = "renderItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"
            ),
            index = 5
    )
    private VertexConsumer enchantment_core$applyItemAlphaConsumer(VertexConsumer originalConsumer) {
        if (this.enchantment_core$currentItemAlpha >= 1.0F) return originalConsumer;

        return new VertexConsumer() {
            @Override
            public VertexConsumer addVertex(float x, float y, float z) { return originalConsumer.addVertex(x, y, z); }

            // Intercept color assignment to scale the discrete alpha parameter.
            @Override
            public VertexConsumer setColor(int r, int g, int b, int a) {
                int newAlpha = (int) (a * enchantment_core$currentItemAlpha);
                return originalConsumer.setColor(r, g, b, newAlpha);
            }

            @Override
            public VertexConsumer setUv(float u, float v) { return originalConsumer.setUv(u, v); }

            @Override
            public VertexConsumer setUv1(int u, int v) { return originalConsumer.setUv1(u, v); }

            @Override
            public VertexConsumer setUv2(int u, int v) { return originalConsumer.setUv2(u, v); }

            @Override
            public VertexConsumer setNormal(float x, float y, float z) { return originalConsumer.setNormal(x, y, z); }
        };
    }
}