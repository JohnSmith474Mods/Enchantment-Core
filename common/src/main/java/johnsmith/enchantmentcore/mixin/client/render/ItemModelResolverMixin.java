package johnsmith.enchantmentcore.mixin.client.render;

import johnsmith.enchantmentcore.api.client.render.ItemRenderStateAlphaAccessor;
import johnsmith.enchantmentcore.client.render.TransparencyRenderHelper;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {

    @Inject(method = "updateForTopItem", at = @At("HEAD"))
    private void enchantment_core$captureAlphaTop(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Level level, LivingEntity entity, int i, CallbackInfo ci) {
        enchantment_core$applyAlpha(state, stack);
    }

    @Inject(method = "updateForNonLiving", at = @At("HEAD"))
    private void enchantment_core$captureAlphaNonLiving(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Entity entity, CallbackInfo ci) {
        enchantment_core$applyAlpha(state, stack);
    }

    @Inject(method = "updateForLiving", at = @At("HEAD"))
    private void enchantment_core$captureAlphaLiving(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, LivingEntity entity, CallbackInfo ci) {
        enchantment_core$applyAlpha(state, stack);
    }

    @Unique
    private void enchantment_core$applyAlpha(ItemStackRenderState state, ItemStack stack) {
        if (state instanceof ItemRenderStateAlphaAccessor accessor) {
            accessor.enchantment_core$setAlpha(TransparencyRenderHelper.calculateAlpha(stack));
        }
    }
}