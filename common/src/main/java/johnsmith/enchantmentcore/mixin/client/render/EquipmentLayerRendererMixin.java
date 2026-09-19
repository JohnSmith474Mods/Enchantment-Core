package johnsmith.enchantmentcore.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import johnsmith.enchantmentcore.client.render.TransparencyRenderHelper;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting the unified equipment rendering pipeline.
 * Applies visual transparency to worn equipment (armor, elytra, trims, etc.) based on active enchantment configurations.
 */
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {

    // Define the type-erased target descriptor constant for clarity and reuse.
    @Unique
    private static final String TARGET_METHOD = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/ResourceLocation;II)V";

    /**
     * Intercepts the SubmitNodeCollector parameter before it is processed by the renderer.
     * Replaces the collector with a wrapped proxy instance that intercepts model submissions to apply alpha transformations.
     *
     * @param nodeCollector The original vanilla node collector.
     * @param item          The item stack being rendered, captured via MixinExtras Local.
     * @return The wrapped proxy node collector, or the original if the alpha multiplier is 1.0F.
     */
    @ModifyVariable(
            method = TARGET_METHOD,
            at = @At("HEAD"),
            argsOnly = true
    )
    private SubmitNodeCollector enchantment_core$wrapCollector(SubmitNodeCollector nodeCollector, @Local(argsOnly = true) ItemStack item) {
        float alpha = TransparencyRenderHelper.calculateAlpha(item);
        if (alpha < 1.0F) {
            TransparencyRenderHelper.setAlphaActive(alpha);
            return TransparencyRenderHelper.wrapCollector(nodeCollector, alpha);
        }
        return nodeCollector;
    }

    /**
     * Purges thread-local transparency state upon completion of the equipment layer render pipeline.
     *
     * @param ci The callback information.
     */
    @Inject(
            method = TARGET_METHOD,
            at = @At("RETURN")
    )
    private void enchantment_core$clearEquipmentAlpha(CallbackInfo ci) {
        TransparencyRenderHelper.clearAlphaActive();
    }

    /**
     * Intercepts the static factory call for armor RenderTypes.
     * Upgrades opaque/cutout armor pipelines to translucent equivalents if an alpha multiplier is active.
     *
     * @param location The resource location of the armor texture.
     * @param original The original method operation.
     * @return The translucent render type if alpha is active, otherwise the original cutout pipeline.
     */
    @WrapOperation(
            method = TARGET_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;")
    )
    private RenderType enchantment_core$switchToTranslucent(ResourceLocation location, Operation<RenderType> original) {
        if (TransparencyRenderHelper.isAlphaActive()) {
            return RenderType.itemEntityTranslucentCull(location);
        }
        return original.call(location);
    }
}