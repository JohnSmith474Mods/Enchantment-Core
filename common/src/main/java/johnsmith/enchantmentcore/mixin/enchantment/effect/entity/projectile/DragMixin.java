package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.projectile;

import johnsmith.enchantmentcore.api.entity.accessor.ProjectileStateAccessor;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Mixin modifying the aerodynamic friction applied to projectiles during flight.
 * Targets both arrows and thrown entities (e.g., snowballs, potions).
 */
@Mixin({AbstractArrow.class, ThrowableProjectile.class})
public abstract class DragMixin {

    /**
     * Intercepts the scalar value passed to {@code Vec3.scale()} during velocity retention calculations.
     *
     * @param originalDrag The vanilla velocity retention factor (e.g., 0.99 for air, 0.6 for water).
     * @return The dynamically modified drag scalar, clamped to 1.0 to prevent infinite acceleration.
     */
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"), index = 0)
    private double enchantment_core$applyDragModifier(double originalDrag) {
        double multiplier = ((ProjectileStateAccessor) this).enchantment_core$getDragMultiplier();
        if (multiplier == 1.0) {
            return originalDrag;
        }

        // Clamp the final scalar to a maximum of 1.0. Exceeding 1.0 causes the projectile
        // to exponentially accelerate every tick.
        return Math.min(1.0, originalDrag * multiplier);
    }
}