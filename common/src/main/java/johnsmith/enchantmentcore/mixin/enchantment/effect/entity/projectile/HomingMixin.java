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
 * Mixin implementing true homing physics for projectiles.
 * Evaluates targeting frustums and continuously recalculates rotational velocity vectors to seek entities.
 */
@Mixin(Projectile.class)
public abstract class HomingMixin {

    @Shadow @Nullable
    public abstract Entity getOwner();

    @Unique private LivingEntity enchantment_core$homingTarget;
    @Unique private int enchantment_core$retargetTimer = 0;
    @Unique private Vec3 enchantment_core$startPosition = null;

    /**
     * Injects at the end of the projectile's tick cycle to apply turning velocity.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void enchantment_core$applyHoming(CallbackInfo ci) {
        Projectile projectile = (Projectile) (Object) this;
        ProjectileStateAccessor state = (ProjectileStateAccessor) projectile;

        double strength = state.enchantment_core$getHomingStrength();
        if (strength <= 0.0) return;

        Entity entity = (Entity) projectile;

        // Capture initialization coordinate to evaluate the arming distance constraint.
        if (this.enchantment_core$startPosition == null) {
            this.enchantment_core$startPosition = entity.position();
        }

        // Suspend homing logic if the projectile has not traveled past its minimum arming distance.
        if (entity.position().distanceToSqr(this.enchantment_core$startPosition) < Math.pow(state.enchantment_core$getHomingArmingDistance(), 2)) {
            return;
        }

        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() < 0.01) return;

        // Refresh the target lock every 10 ticks or if the current target is lost/dead.
        if (this.enchantment_core$homingTarget == null || !this.enchantment_core$homingTarget.isAlive() || this.enchantment_core$retargetTimer-- <= 0) {
            this.enchantment_core$retargetTimer = 10;
            this.enchantment_core$findTargetFrustum(projectile, state);
        }

        if (this.enchantment_core$homingTarget != null) {
            boolean headshot = state.enchantment_core$getHomingPrioritizesHead();
            Vec3 targetPos = headshot ? this.enchantment_core$homingTarget.getEyePosition() : this.enchantment_core$homingTarget.getBoundingBox().getCenter();

            double speed = velocity.length();
            Vec3 desiredDir = targetPos.subtract(entity.position()).normalize();
            Vec3 currentDir = velocity.normalize();

            // Calculate the angular delta between the current flight path and the target.
            double angleToTarget = Math.acos(Math.max(-1.0, Math.min(1.0, currentDir.dot(desiredDir))));
            double maxTurn = Math.toRadians(state.enchantment_core$getHomingTurnRate());

            // Clamp the turn angle by the maximum turn rate per tick.
            double actualTurn = Math.min(angleToTarget * strength, maxTurn);

            if (actualTurn > 0.001) {
                // Compute rotational axis via cross product of current heading and desired heading.
                Vec3 right = currentDir.cross(desiredDir);
                if (right.lengthSqr() > 1e-7) {
                    right = right.normalize();
                    Vec3 up = right.cross(currentDir).normalize();

                    // Rotate the current direction vector toward the desired direction.
                    Vec3 newDir = currentDir.scale(Math.cos(actualTurn)).add(up.scale(Math.sin(actualTurn))).normalize();

                    entity.setDeltaMovement(newDir.scale(speed));
                    entity.hasImpulse = true; // Flags the engine to smooth the visual client interpolation.
                }
            }
        }
    }

    /**
     * Executes a conical frustum search originating from the projectile's nose to locate valid targets.
     */
    @Unique
    private void enchantment_core$findTargetFrustum(Projectile projectile, ProjectileStateAccessor state) {
        double maxDist = state.enchantment_core$getHomingMaxDistance();
        AABB box = projectile.getBoundingBox().inflate(maxDist);
        List<LivingEntity> list = projectile.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this.getOwner() && e.isAlive());

        LivingEntity bestTarget = null;
        double smallestAngle = Double.MAX_VALUE;

        // Establish the local coordinate frame.
        Vec3 forward = projectile.getDeltaMovement().normalize();
        Vec3 globalUp = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(globalUp);
        if (right.lengthSqr() < 1e-5) right = forward.cross(new Vec3(1, 0, 0));
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        double minDistance = state.enchantment_core$getHomingMinDistance();
        double tanFov = Math.tan(Math.toRadians(state.enchantment_core$getHomingFov() / 2.0));

        for (LivingEntity target : list) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(projectile.position());
            double distZ = toTarget.dot(forward);

            // Filter out targets outside depth bounds.
            if (distZ < minDistance || distZ > maxDist) continue;

            double distX = toTarget.dot(right);
            double distY = toTarget.dot(up);
            double limit = distZ * tanFov;

            // Filter out targets outside the field of view cone.
            if (Math.abs(distX) > limit || Math.abs(distY) > limit) continue;

            // Select the target nearest to the direct center of the crosshair.
            double dotProduct = forward.dot(toTarget.normalize());
            double angleDeg = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dotProduct))));

            if (angleDeg < smallestAngle) {
                smallestAngle = angleDeg;
                bestTarget = target;
            }
        }

        this.enchantment_core$homingTarget = bestTarget;
    }
}