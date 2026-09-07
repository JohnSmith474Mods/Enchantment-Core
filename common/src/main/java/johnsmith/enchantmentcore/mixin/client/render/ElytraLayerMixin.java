package johnsmith.enchantmentcore.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.enchantment.effect.TransparencyEffect;

import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
 * Mixin targeting the rendering layer for Elytra.
 * Applies visual transparency to worn Elytra items based on active enchantment configurations.
 */
@Mixin(ElytraLayer.class)
public abstract class ElytraLayerMixin {

    /** State tracker to hold the calculated alpha multiplier during the current render pass. */
    @Unique
    private float enchantment_core$currentAlpha = 1.0F;

    /**
     * Intercepts the start of the Elytra rendering pipeline.
     * Evaluates the item in the chest slot for TRANSPARENCY effects and caches the lowest multiplier.
     *
     * @param poseStack       The active transformation matrix stack.
     * @param bufferSource    The buffer source.
     * @param packedLight     The light level.
     * @param livingEntity    The entity wearing the Elytra.
     * @param limbSwing       The limb swing animation progress.
     * @param limbSwingAmount The limb swing animation amplitude.
     * @param partialTick     The fractional tick value.
     * @param ageInTicks      The entity's age.
     * @param netHeadYaw      The entity's head yaw.
     * @param headPitch       The entity's head pitch.
     * @param ci              The callback information.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void enchantment_core$captureElytraAlpha(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, LivingEntity livingEntity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        this.enchantment_core$currentAlpha = 1.0F;
        ItemStack stack = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
        if (stack.isEmpty()) return;

        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;

        float minAlpha = 1.0F;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            List<ConditionalEffect<TransparencyEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.TRANSPARENCY);
            if (effects != null) {
                for (ConditionalEffect<TransparencyEffect> cond : effects) {
                    minAlpha = Math.min(minAlpha, cond.effect().alphaMultiplier().calculate(entry.getIntValue()));
                }
            }
        }
        this.enchantment_core$currentAlpha = Math.max(0.0F, Math.min(1.0F, minAlpha));
    }

    /**
     * Redirects the final buffer rendering call for the Elytra model.
     * Injects the calculated alpha value into the color integer.
     */
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ElytraModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V")
    )
    private void enchantment_core$redirectElytraRender(ElytraModel model, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay) {
        // Convert the float alpha back into the 0-255 range and bitshift it into the highest byte (alpha channel).
        int a = (int) (255.0F * this.enchantment_core$currentAlpha);
        int color = (a << 24) | 0x00FFFFFF;

        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}