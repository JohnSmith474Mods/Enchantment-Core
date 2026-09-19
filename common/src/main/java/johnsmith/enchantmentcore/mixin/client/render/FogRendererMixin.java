package johnsmith.enchantmentcore.mixin.client.render;

import java.util.Map;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.enchantment.effect.FluidFogDensityEffect;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting the ambient environment fog renderer.
 * Mutates fog rendering distances when submerged in specific fluids to simulate enhanced visibility effects.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    /**
     * Intercepts the end of the fog setup process to forcefully override the returned parameters.
     * Evaluates the equipped helmet for active FLUID_FOG_DENSITY effects.
     *
     * @param camera       The player camera instance.
     * @param fogMode      The active fog rendering mode.
     * @param fogColor     The evaluated base fog color vector.
     * @param viewDistance The current configured client view distance.
     * @param thickFog     Whether the environment enforces thick fog (e.g., Nether).
     * @param tickDelta    The fractional tick value.
     * @param cir          The callback information returning the FogParameters record.
     */
    @Inject(method = "setupFog", at = @At("RETURN"), cancellable = true)
    private static void applyLevelBasedWaterVisibility(
            Camera camera,
            FogRenderer.FogMode fogMode,
            Vector4f fogColor,
            float viewDistance,
            boolean thickFog,
            float tickDelta,
            CallbackInfoReturnable<FogParameters> cir
    ) {
        // Extract required context directly from the Camera parameter
        FogType fogType = camera.getFluidInCamera();
        Entity entity = camera.getEntity();

        if (fogType == FogType.NONE || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        ItemStack helmet = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(helmet);

        if (enchantments.isEmpty()) return;

        FluidState fluidState = livingEntity.level().getFluidState(camera.getBlockPosition());

        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
            FluidFogDensityEffect effect = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.FLUID_FOG_DENSITY.get());

            if (effect != null && fluidState.is(effect.fluids())) {
                int level = entry.getValue();

                float start = effect.fogStart().calculate(level);
                float endMultiplier = effect.fogEndMultiplier().calculate(level);

                FogParameters original = cir.getReturnValue();

                // Construct a new FogParameters record with modified bounds
                cir.setReturnValue(new FogParameters(
                        start,
                        viewDistance * endMultiplier,
                        original.shape(),
                        fogColor.x,
                        fogColor.y,
                        fogColor.z,
                        fogColor.w
                ));
                return;
            }
        }
    }
}