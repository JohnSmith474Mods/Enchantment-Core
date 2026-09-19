package johnsmith.enchantmentcore.mixin.client.render;

import johnsmith.enchantmentcore.client.render.TransparencyRenderHelper;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts static factory methods in the RenderType class.
 * Substitutes opaque and cutout render pipelines with translucent equivalents if an alpha multiplier is active.
 */
@Mixin(RenderTypes.class)
public class RenderTypeMixin {

    /**
     * Intercepts the entitySolid render type request.
     * Substitutes the render type with itemEntityTranslucentCull if an alpha multiplier is active.
     *
     * @param location The resource location of the texture.
     * @param cir      The callback information returnable.
     */
    @Inject(method = "entitySolid(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), cancellable = true)
    private static void enchantment_core$upgradeEntitySolid(Identifier location, CallbackInfoReturnable<RenderType> cir) {
        if (TransparencyRenderHelper.isAlphaActive()) {
            cir.setReturnValue(RenderTypes.itemEntityTranslucentCull(location));
        }
    }

    /**
     * Intercepts the entityCutout render type request.
     * Substitutes the render type with itemEntityTranslucentCull if an alpha multiplier is active.
     *
     * @param location The resource location of the texture.
     * @param cir      The callback information returnable.
     */
    @Inject(method = "entityCutout(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), cancellable = true)
    private static void enchantment_core$upgradeEntityCutout(Identifier location, CallbackInfoReturnable<RenderType> cir) {
        if (TransparencyRenderHelper.isAlphaActive()) {
            cir.setReturnValue(RenderTypes.itemEntityTranslucentCull(location));
        }
    }

    /**
     * Intercepts the entityCutoutNoCull render type request.
     * Substitutes the render type with entityTranslucent if an alpha multiplier is active.
     * Preserves the no-cull geometry property.
     *
     * @param location The resource location of the texture.
     * @param cir      The callback information returnable.
     */
    @Inject(method = "entityCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), cancellable = true)
    private static void enchantment_core$upgradeEntityCutoutNoCull(Identifier location, CallbackInfoReturnable<RenderType> cir) {
        if (TransparencyRenderHelper.isAlphaActive()) {
            // Reverts to standard translucent to preserve the No-Cull geometry contract
            cir.setReturnValue(RenderTypes.entityTranslucent(location));
        }
    }
}