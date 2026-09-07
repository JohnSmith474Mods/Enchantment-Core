package johnsmith.enchantmentcore.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.enchantment.effect.TransparencyEffect;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting the humanoid armor rendering layer.
 * Facilitates variable transparency for equipped armor pieces to visually represent stealth or evasion effects.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    /** State tracker to hold the calculated alpha multiplier during the current render pass. */
    @Unique
    private float enchantment_core$currentAlpha = 1.0F;

    /**
     * Captures the initial render call for each armor piece to evaluate transparency requirements.
     *
     * @param pPoseStack     The active transformation matrix stack.
     * @param pBufferSource  The buffer source.
     * @param pLivingEntity  The entity wearing the armor.
     * @param pSlot          The equipment slot being rendered.
     * @param pPackedLight   The light level.
     * @param pModel         The humanoid model instance.
     * @param ci             The callback information.
     */
    @Inject(method = "renderArmorPiece", at = @At("HEAD"))
    private void enchantment_core$captureArmorAlpha(PoseStack pPoseStack, MultiBufferSource pBufferSource, LivingEntity pLivingEntity, EquipmentSlot pSlot, int pPackedLight, HumanoidModel<?> pModel, CallbackInfo ci) {
        this.enchantment_core$currentAlpha = 1.0F;
        ItemStack stack = pLivingEntity.getItemBySlot(pSlot);
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
     * Swaps the RenderType mapping for the armor geometry.
     * Upgrades the standard cutout renderer to a translucent implementation to prevent sorting errors
     * when the transparent armor overlays complex environments (like water).
     */
    @Redirect(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType enchantment_core$switchToTranslucent(ResourceLocation texture) {
        if (this.enchantment_core$currentAlpha < 1.0F) {
            return RenderType.itemEntityTranslucentCull(texture);
        }
        return RenderType.armorCutoutNoCull(texture);
    }

    /**
     * Modifies the color parameter supplied to the model buffer renderer.
     * Dynamically scales the alpha channel bits to uniformly apply the transparency effect across the base model, trims, and glint.
     * Targets the implementation signature (PoseStack, VertexConsumer, packedLight, packedOverlay, color).
     *
     * @param originalColor The original ARGB color integer.
     * @return The modified ARGB color integer.
     */
    @ModifyArg(
            method = {"renderModel", "renderTrim", "renderGlint"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            index = 4
    )
    private int enchantment_core$applyAlphaToAll(int originalColor) {
        if (this.enchantment_core$currentAlpha >= 1.0F) return originalColor;

        // Extract the original alpha byte, scale it, and mask it back into the integer alongside the RGB bytes.
        int a = (int) (((originalColor >> 24) & 0xFF) * this.enchantment_core$currentAlpha);
        return (originalColor & 0x00FFFFFF) | (a << 24);
    }
}