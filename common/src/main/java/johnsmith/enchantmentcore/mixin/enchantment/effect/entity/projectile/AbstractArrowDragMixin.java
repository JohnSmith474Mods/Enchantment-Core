package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.projectile;

import johnsmith.enchantmentcore.api.entity.accessor.ProjectileStateAccessor;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Mixin modifying the aerodynamic friction applied to arrows during flight.
 * Intercepts the velocity retention calculations within the inertia processing phase.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowDragMixin {

    /**
     * Intercepts the scalar value passed to the vector scaling function during velocity retention calculations.
     * Evaluates the projectile state for active drag modification enchantments.
     *
     * @param originalDrag The default velocity retention factor defined by the environment.
     * @return The dynamically modified drag scalar. Clamped to a maximum of 1.0 to prevent infinite acceleration.
     */
    @ModifyArg(
            method = "applyInertia",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"
            ),
            index = 0
    )
    private double enchantment_core$applyDragModifier(double originalDrag) {
        double multiplier = ((ProjectileStateAccessor) this).enchantment_core$getDragMultiplier();
        if (multiplier == 1.0) {
            return originalDrag;
        }

        return Math.min(1.0, originalDrag * multiplier);
    }
}