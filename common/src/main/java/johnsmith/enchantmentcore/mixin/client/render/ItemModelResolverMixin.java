package johnsmith.enchantmentcore.mixin.client.render;

import johnsmith.enchantmentcore.api.client.render.ItemRenderStateAlphaAccessor;
import johnsmith.enchantmentcore.client.render.TransparencyRenderHelper;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects alpha calculation and cache segregation logic into the item model resolution pipeline.
 */
@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {

    /**
     * Intercepts standard item model resolution to capture and evaluate alpha state.
     *
     * @param renderState    The target render state instance.
     * @param stack          The item stack being evaluated.
     * @param displayContext The display context of the item.
     * @param level          The client level context.
     * @param owner          The entity owning the item.
     * @param seed           The random seed.
     * @param ci             The callback information.
     */
    @Inject(method = "updateForTopItem", at = @At("HEAD"))
    private void enchantment_core$captureAlphaTop(ItemStackRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, Level level, ItemOwner owner, int seed, CallbackInfo ci) {
        enchantment_core$processAlphaState(renderState, stack);
    }

    /**
     * Intercepts non-living entity item model resolution to capture and evaluate alpha state.
     *
     * @param state   The target render state instance.
     * @param stack   The item stack being evaluated.
     * @param context The display context of the item.
     * @param entity  The non-living entity context.
     * @param ci      The callback information.
     */
    @Inject(method = "updateForNonLiving", at = @At("HEAD"))
    private void enchantment_core$captureAlphaNonLiving(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, Entity entity, CallbackInfo ci) {
        enchantment_core$processAlphaState(state, stack);
    }

    /**
     * Intercepts living entity item model resolution to capture and evaluate alpha state.
     *
     * @param state   The target render state instance.
     * @param stack   The item stack being evaluated.
     * @param context The display context of the item.
     * @param entity  The living entity context.
     * @param ci      The callback information.
     */
    @Inject(method = "updateForLiving", at = @At("HEAD"))
    private void enchantment_core$captureAlphaLiving(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, LivingEntity entity, CallbackInfo ci) {
        enchantment_core$processAlphaState(state, stack);
    }

    /**
     * Calculates the alpha multiplier and assigns it to the target render state accessor.
     * Appends the calculated alpha to the model identity element to construct isolated cache keys for varying transparency levels.
     *
     * @param state The target render state instance.
     * @param stack The item stack to evaluate.
     */
    @Unique
    private void enchantment_core$processAlphaState(ItemStackRenderState state, ItemStack stack) {
        float alpha = TransparencyRenderHelper.calculateAlpha(stack);
        state.appendModelIdentityElement(alpha);

        if (state instanceof ItemRenderStateAlphaAccessor accessor) {
            accessor.enchantment_core$setAlpha(alpha);
        }
    }
}