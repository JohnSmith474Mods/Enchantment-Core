package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.projectile;

import java.util.List;

import johnsmith.enchantmentcore.api.entity.accessor.ProjectileStateAccessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin implementing simple magnetic attraction for projectiles.
 * Instantly shifts the projectile's trajectory vector without restrictive turn rates or FOV cones.
 */
@Mixin(Projectile.class)
public abstract class MagneticMixin {

    @Shadow @Nullable
    public abstract Entity getOwner();

    @Unique private LivingEntity enchantment_core$magneticTarget;
    @Unique private int enchantment_core$magneticRetargetTimer = 0;
    @Unique private Vec3 enchantment_core$startPosition = null;

    /**
     * Evaluates magnetic attraction logic at the end of the tick cycle.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void enchantment_core$applyMagnetism(CallbackInfo ci) {
        Projectile projectile = (Projectile) (Object) this;
        ProjectileStateAccessor state = (ProjectileStateAccessor) projectile;

        double strength = state.enchantment_core$getMagneticStrength();
        if (strength <= 0.0) return;

        if (this.enchantment_core$startPosition == null) {
            this.enchantment_core$startPosition = projectile.position();
        }

        // Suppress logic prior to reaching arming distance.
        if (projectile.position().distanceToSqr(this.enchantment_core$startPosition) < Math.pow(state.enchantment_core$getMagneticArmingDistance(), 2)) {
            return;
        }

        Vec3 velocity = projectile.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 0.01) return;

        if (this.enchantment_core$magneticTarget == null || !this.enchantment_core$magneticTarget.isAlive() || this.enchantment_core$magneticRetargetTimer-- <= 0) {
            this.enchantment_core$magneticRetargetTimer = 10;
            this.enchantment_core$findMagneticTarget(projectile, state);
        }

        if (this.enchantment_core$magneticTarget != null) {
            boolean headshot = state.enchantment_core$getMagneticPrioritizesHead();
            Vec3 targetPos = headshot ? this.enchantment_core$magneticTarget.getEyePosition() : this.enchantment_core$magneticTarget.getBoundingBox().getCenter();

            // Generate the direct vector to the target and aggressively interpolate the current flight path.
            Vec3 toTarget = targetPos.subtract(((Entity) projectile).position()).normalize();
            Vec3 newVelocity = velocity.add(toTarget.scale(strength)).normalize().scale(speed);

            projectile.setDeltaMovement(newVelocity);
            projectile.hasImpulse = true;
        }
    }

    /**
     * Executes a simple proximity-based radial search for targets.
     */
    @Unique
    private void enchantment_core$findMagneticTarget(Projectile projectile, ProjectileStateAccessor state) {
        double radius = state.enchantment_core$getMagneticSearchRadius();
        AABB box = projectile.getBoundingBox().inflate(radius);
        List<LivingEntity> list = projectile.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this.getOwner() && e.isAlive());

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity target : list) {
            double dist = projectile.distanceToSqr(target);
            if (dist < closestDist) {
                closestDist = dist;
                closest = target;
            }
        }

        this.enchantment_core$magneticTarget = closest;
    }
}