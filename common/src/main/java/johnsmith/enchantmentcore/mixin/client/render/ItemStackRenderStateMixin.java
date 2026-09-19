package johnsmith.enchantmentcore.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import johnsmith.enchantmentcore.api.client.render.ItemRenderStateAlphaAccessor;
import johnsmith.enchantmentcore.client.render.TransparencyRenderHelper;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateMixin implements ItemRenderStateAlphaAccessor {
    @Unique
    private float enchantment_core$alpha = 1.0F;

    @Override
    public void enchantment_core$setAlpha(float alpha) {
        this.enchantment_core$alpha = alpha;
    }

    @Override
    public float enchantment_core$getAlpha() {
        return this.enchantment_core$alpha;
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private MultiBufferSource enchantment_core$wrapBuffer(MultiBufferSource buffer) {
        if (this.enchantment_core$alpha < 1.0F) {
            TransparencyRenderHelper.setAlphaActive(this.enchantment_core$alpha);
            return TransparencyRenderHelper.wrapBuffer(buffer, this.enchantment_core$alpha);
        }
        return buffer;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void enchantment_core$cleanupAlpha(PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
        TransparencyRenderHelper.clearAlphaActive();
    }
}