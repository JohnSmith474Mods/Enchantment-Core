package johnsmith.enchantmentcore.mixin.world.damagesource;

import johnsmith.enchantmentcore.config.Config;

import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting CombatRules to parameterize the Enchantment Protection Factor (EPF) calculation.
 * Exposes the hardcoded vanilla protection bounds and damage reduction divisor to the configuration API.
 * Centralizes the injection point to prevent compatibility conflicts across dependent mods.
 */
@Mixin(CombatRules.class)
public class CombatRulesMixin {

    /**
     * Intercepts the magic absorption damage reduction calculation.
     * Replaces the static vanilla parameters (maximum protection: 20.0, reduction divisor: 25.0)
     * with dynamic configuration endpoints.
     *
     * @param damage     The incoming damage amount.
     * @param protection The aggregated Enchantment Protection Factor (EPF) from equipped armor.
     * @param cir        The callback information storing the resulting damage.
     */
    @Inject(method = "getDamageAfterMagicAbsorb(FF)F", at = @At("HEAD"), cancellable = true)
    private static void enchantment_core$parameterizeProtectionCalculation(float damage, float protection, CallbackInfoReturnable<Float> cir) {
        float maxProtection = Config.BOUNDED_PROTECTION_NUMERATOR.get();
        float divisor = Config.BOUNDED_PROTECTION_DENOMINATOR.get();
        divisor = divisor <= maxProtection ? maxProtection * 1.2F : divisor;

        float f = Mth.clamp(protection, 0.0F, maxProtection);
        float newDamage = damage * (1.0F - f / divisor);

        cir.setReturnValue(newDamage);
    }
}