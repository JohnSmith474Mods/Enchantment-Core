package johnsmith.enchantmentcore.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import johnsmith.enchantmentcore.client.render.TransparencyRenderHelper;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting the unified equipment rendering pipeline.
 * Applies visual transparency to worn equipment (armor, elytra, etc.) based on active enchantment configurations.
 */
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {

    @Inject(method = "renderLayers*", at = @At("HEAD"))
    private void enchantment_core$captureEquipmentAlpha(CallbackInfo ci, @Local(argsOnly = true) ItemStack itemStack) {
        float alpha = TransparencyRenderHelper.calculateAlpha(itemStack);
        if (alpha < 1.0F) {
            TransparencyRenderHelper.setAlphaActive(alpha);
        }
    }

    @Inject(method = "renderLayers*", at = @At("RETURN"))
    private void enchantment_core$clearEquipmentAlpha(CallbackInfo ci) {
        TransparencyRenderHelper.clearAlphaActive();
    }

    @WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
            )
    )
    private RenderType enchantment_core$switchToTranslucent(ResourceLocation location, Operation<RenderType> original) {
        if (TransparencyRenderHelper.isAlphaActive()) {
            return RenderType.itemEntityTranslucentCull(location);
        }
        return original.call(location);
    }

    @WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
            )
    )
    private void enchantment_core$applyAlphaToEquipment(Model instance, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color, Operation<Void> original) {
        float alpha = TransparencyRenderHelper.getAlpha();
        if (alpha >= 1.0F) {
            original.call(instance, poseStack, buffer, packedLight, packedOverlay, color);
            return;
        }

        // Downscales the alpha component (bits 24-31) of the ARGB integer
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        int modifiedColor = (color & 0x00FFFFFF) | (a << 24);
        original.call(instance, poseStack, buffer, packedLight, packedOverlay, modifiedColor);
    }
}