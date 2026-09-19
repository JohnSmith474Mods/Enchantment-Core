package johnsmith.enchantmentcore.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import johnsmith.enchantmentcore.api.client.render.ItemRenderStateAlphaAccessor;
import johnsmith.enchantmentcore.client.render.TransparencyRenderHelper;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects alpha state storage and node collector wrapping into the item render state.
 * Implements {@link ItemRenderStateAlphaAccessor} to manage visual transparency limits.
 */
@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateMixin implements ItemRenderStateAlphaAccessor {
    @Unique
    private float enchantment_core$alpha = 1.0F;

    /**
     * Sets the internal alpha multiplier for the current render state.
     *
     * @param alpha The alpha multiplier to apply.
     */
    @Override
    public void enchantment_core$setAlpha(float alpha) {
        this.enchantment_core$alpha = alpha;
    }

    /**
     * Retrieves the stored alpha multiplier for the current render state.
     *
     * @return The stored alpha multiplier.
     */
    @Override
    public float enchantment_core$getAlpha() {
        return this.enchantment_core$alpha;
    }

    /**
     * Intercepts the asynchronous node collector parameter prior to processing.
     * Substitutes the collector with a wrapped instance that intercepts geometry submissions to apply alpha transformations.
     *
     * @param nodeCollector The original vanilla node collector.
     * @return The wrapped proxy node collector, or the original if the alpha multiplier is 1.0F.
     */
    @ModifyVariable(method = "submit", at = @At("HEAD"), argsOnly = true)
    private SubmitNodeCollector enchantment_core$wrapCollector(SubmitNodeCollector nodeCollector) {
        if (this.enchantment_core$alpha < 1.0F) {
            TransparencyRenderHelper.setAlphaActive(this.enchantment_core$alpha);
            return TransparencyRenderHelper.wrapCollector(nodeCollector, this.enchantment_core$alpha);
        }
        return nodeCollector;
    }

    /**
     * Purges thread-local transparency state upon completion of the submit pipeline.
     *
     * @param poseStack     The active matrix stack.
     * @param nodeCollector The active node collector.
     * @param packedLight   The calculated light level.
     * @param packedOverlay The calculated overlay level.
     * @param outlineColor  The outline color integer.
     * @param ci            The callback information.
     */
    @Inject(method = "submit", at = @At("RETURN"))
    private void enchantment_core$cleanupAlpha(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, int outlineColor, CallbackInfo ci) {
        TransparencyRenderHelper.clearAlphaActive();
    }
}