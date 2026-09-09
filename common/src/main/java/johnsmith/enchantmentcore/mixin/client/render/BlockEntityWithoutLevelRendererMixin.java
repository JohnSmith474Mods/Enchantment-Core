package johnsmith.enchantmentcore.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.enchantment.effect.TransparencyEffect;

import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting the BlockEntityWithoutLevelRenderer (BEWLR).
 * Intercepts and applies transparency enchantment effects to complex items
 * utilizing custom block entity renderers, including Shields and Tridents.
 */
@Mixin(BlockEntityWithoutLevelRenderer.class)
public abstract class BlockEntityWithoutLevelRendererMixin {

    /** State tracker containing the calculated alpha multiplier during the current render pass. */
    @Unique
    private float enchantment_core$currentAlpha = 1.0F;

    /**
     * Intercept the initial render call to evaluate the item enchantments.
     * Calculate and cache the minimum active alpha multiplier from the TRANSPARENCY component.
     *
     * @param pStack          The item stack undergoing rendering.
     * @param pDisplayContext The rendering context.
     * @param pPoseStack      The active transformation matrix stack.
     * @param pBuffer         The buffer source.
     * @param pPackedLight    The packed light integer.
     * @param pPackedOverlay  The packed overlay integer.
     * @param ci              The callback information.
     */
    @Inject(method = "renderByItem", at = @At("HEAD"))
    private void enchantment_core$captureItemAlpha(ItemStack pStack, ItemDisplayContext pDisplayContext, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay, CallbackInfo ci) {
        this.enchantment_core$currentAlpha = 1.0F;
        if (pStack.isEmpty()) return;

        ItemEnchantments enchantments = pStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
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

        this.enchantment_core$currentAlpha = Math.max(0.0F, Math.min(1.0F, minAlpha));
    }

    /**
     * Redirect the render type resolution for Shields.
     * Enforce a translucent cull render type if the cached alpha multiplier falls below 1.0.
     *
     * @param instance The shield model instance.
     * @param texture  The resource location of the shield texture.
     * @return The resolved render type.
     */
    @Redirect(
            method = "renderByItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ShieldModel;renderType(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;")
    )
    private RenderType enchantment_core$shieldTranslucent(ShieldModel instance, ResourceLocation texture) {
        if (this.enchantment_core$currentAlpha < 1.0F) return RenderType.itemEntityTranslucentCull(texture);
        return instance.renderType(texture);
    }

    /**
     * Redirect the render type resolution for Tridents.
     * Enforce a translucent cull render type if the cached alpha multiplier falls below 1.0.
     *
     * @param instance The trident model instance.
     * @param texture  The resource location of the trident texture.
     * @return The resolved render type.
     */
    @Redirect(
            method = "renderByItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/TridentModel;renderType(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;")
    )
    private RenderType enchantment_core$tridentTranslucent(TridentModel instance, ResourceLocation texture) {
        if (this.enchantment_core$currentAlpha < 1.0F) return RenderType.itemEntityTranslucentCull(texture);
        return instance.renderType(texture);
    }

    /**
     * Intercept the vertex consumer retrieval from the ItemRenderer.
     * Wrap the consumer to scale the alpha parameter of submitted vertices by the cached multiplier.
     *
     * @param bufferSource The active buffer source.
     * @param renderType   The requested render type.
     * @param hasEntry     Whether the buffer has an entry.
     * @param hasFoil      Whether the item has a glint effect.
     * @return The modified or original vertex consumer.
     */
    @Redirect(
            method = "renderByItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private VertexConsumer enchantment_core$applyBEWLRAlpha(MultiBufferSource bufferSource, RenderType renderType, boolean hasEntry, boolean hasFoil) {
        VertexConsumer originalConsumer = ItemRenderer.getFoilBuffer(bufferSource, renderType, hasEntry, hasFoil);
        if (this.enchantment_core$currentAlpha >= 1.0F) return originalConsumer;

        return new VertexConsumer() {
            @Override public VertexConsumer addVertex(float x, float y, float z) { originalConsumer.addVertex(x, y, z); return this; }

            @Override public VertexConsumer setColor(int r, int g, int b, int a) { originalConsumer.setColor(r, g, b, (int) (a * enchantment_core$currentAlpha)); return this; }

            @Override public VertexConsumer setUv(float u, float v) { originalConsumer.setUv(u, v); return this; }
            @Override public VertexConsumer setUv1(int u, int v) { originalConsumer.setUv1(u, v); return this; }
            @Override public VertexConsumer setUv2(int u, int v) { originalConsumer.setUv2(u, v); return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { originalConsumer.setNormal(x, y, z); return this; }
        };
    }
}