package johnsmith.enchantmentcore.mixin.enchantment.effect.entity;

import johnsmith.enchantmentcore.api.entity.accessor.EntityStateAccessor;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting the base Entity class to intercept gravity calculations.
 * Checks for the presence of the ProjectileStateAccessor interface to dynamically scale gravity forces.
 */
@Mixin(Entity.class)
public abstract class GravityMixin implements EntityStateAccessor {

    @Unique private Double enchantment_core$gravityMultiplier = null;

    @Override public void enchantment_core$setGravityMultiplier(double multiplier) { this.enchantment_core$gravityMultiplier = multiplier; }

    @Override public double enchantment_core$getGravityMultiplier() { return this.enchantment_core$gravityMultiplier != null ? this.enchantment_core$gravityMultiplier : 1.0; }

    /**
     * Intercepts the entity gravity evaluation cycle.
     * Mutates the returned gravity factor if the evaluating instance possesses an active projectile multiplier.
     *
     * @param cir Callback information storing the baseline gravity value.
     */
    @Inject(method = "getGravity", at = @At("RETURN"), cancellable = true)
    private void enchantment_core$applyGravityModifier(CallbackInfoReturnable<Double> cir) {
        if (this.enchantment_core$gravityMultiplier != null) {
            cir.setReturnValue(cir.getReturnValue() * this.enchantment_core$gravityMultiplier);
        }
    }
}