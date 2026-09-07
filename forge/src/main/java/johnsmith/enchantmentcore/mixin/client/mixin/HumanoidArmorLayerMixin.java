package johnsmith.enchantmentcore.mixin.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import johnsmith.enchantmentcore.enchantment.effect.TransparencyEffect;
import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @Unique
    private float enchantment_core$currentAlpha = 1.0F;

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

    @WrapOperation(
            method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;")
    )
    private RenderType enchantment_core$switchToTranslucent(ResourceLocation location, Operation<RenderType> original) {
        if (this.enchantment_core$currentAlpha < 1.0F) {
            return RenderType.itemEntityTranslucentCull(location);
        }
        return original.call(location);
    }

    @WrapOperation(
            method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V")
    )
    private void enchantment_core$applyAlphaToModel(Model instance, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color, Operation<Void> original) {
        if (this.enchantment_core$currentAlpha >= 1.0F) {
            original.call(instance, poseStack, buffer, packedLight, packedOverlay, color);
            return;
        }
        int a = (int) (((color >> 24) & 0xFF) * this.enchantment_core$currentAlpha);
        int modifiedColor = (color & 0x00FFFFFF) | (a << 24);
        original.call(instance, poseStack, buffer, packedLight, packedOverlay, modifiedColor);
    }

    @WrapOperation(
            method = {
                    "renderTrim(Lnet/minecraft/core/Holder;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/armortrim/ArmorTrim;Lnet/minecraft/client/model/Model;Z)V",
                    "renderGlint(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/HumanoidModel;)V"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V")
    )
    private void enchantment_core$applyAlphaToTrimAndGlint(Model instance, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, Operation<Void> original) {
        if (this.enchantment_core$currentAlpha >= 1.0F) {
            original.call(instance, poseStack, buffer, packedLight, packedOverlay);
            return;
        }
        int a = (int) (255 * this.enchantment_core$currentAlpha);
        int modifiedColor = (0x00FFFFFF) | (a << 24);
        instance.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, modifiedColor);
    }
}