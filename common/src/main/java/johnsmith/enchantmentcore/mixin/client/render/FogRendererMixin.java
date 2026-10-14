package johnsmith.enchantmentcore.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Local;

import java.util.Map;

import johnsmith.enchantmentcore.api.registry.EnchantmentEffectComponents;
import johnsmith.enchantmentcore.api.enchantment.effect.FluidFogDensityEffect;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
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
     * @param cir          The callback information returning the mutable FogData struct.
     * @param camera       The player camera instance resolved via Local capture.
     * @param viewDistance The current configured client view distance resolved via Local capture.
     */
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void enchantment_core$applyLevelBasedWaterVisibility(
            CallbackInfoReturnable<FogData> cir,
            @Local(argsOnly = true) Camera camera,
            @Local(argsOnly = true) float viewDistance
    ) {
        // 1. Extract required context directly from the Camera parameter
        FogType fogType = camera.getFluidInCamera();
        Entity entity = camera.entity();

        if (fogType == FogType.NONE || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        ItemStack helmet = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(helmet);

        if (enchantments.isEmpty()) return;

        // 2. Retrieve the actual FluidState from the world using the camera position.
        FluidState fluidState = livingEntity.level().getFluidState(camera.blockPosition());

        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
            FluidFogDensityEffect effect = entry.getKey().value().effects().get(EnchantmentEffectComponents.FLUID_FOG_DENSITY);

            // 3. Check if the current fluid matches the effect's configuration.
            if (effect != null && fluidState.is(effect.fluids())) {
                int level = entry.getValue();

                // 4. Calculate dynamic values for the fog boundaries.
                float start = effect.fogStart().calculate(level);
                float end = viewDistance * effect.fogEndMultiplier().calculate(level);

                // 5. Construct a new FogParameters record with modified bounds
                FogData fogData = cir.getReturnValue();
                fogData.environmentalStart = start;
                fogData.renderDistanceStart = start;
                fogData.environmentalEnd = end;
                fogData.renderDistanceEnd = end;
                return;
            }
        }
    }
}