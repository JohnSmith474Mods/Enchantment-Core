package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.projectile;

import johnsmith.enchantmentcore.api.entity.accessor.ProjectileStateAccessor;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin altering the block collision physics of arrows to enable ricochets.
 */
@Mixin(AbstractArrow.class)
public abstract class RicochetMixin {

    /**
     * Intercepts the block collision event before the arrow anchors itself to the geometry.
     *
     * @param hitResult The collision raycast result containing the impacted face direction.
     * @param ci        The callback information used to cancel the vanilla block-sticking mechanics.
     */
    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void enchantment_core$handleRicochet(BlockHitResult hitResult, CallbackInfo ci) {
        ProjectileStateAccessor state = (ProjectileStateAccessor) this;
        int bounces = state.enchantment_core$getBouncesRemaining();

        if (bounces > 0) {
            state.enchantment_core$setBouncesRemaining(bounces - 1);

            // Cast down to the base Entity class to manipulate raw velocity arrays.
            net.minecraft.world.entity.Entity entity = (net.minecraft.world.entity.Entity) (Object) this;
            Vec3 velocity = entity.getDeltaMovement();
            Direction.Axis axis = hitResult.getDirection().getAxis();

            double vx = velocity.x;
            double vy = velocity.y;
            double vz = velocity.z;

            // Invert the velocity vector directly across the impacted planar axis.
            if (axis == Direction.Axis.X) vx = -vx;
            else if (axis == Direction.Axis.Y) vy = -vy;
            else if (axis == Direction.Axis.Z) vz = -vz;

            // Multiply the resulting vector by the kinetic retention coefficient.
            Vec3 reflectedVelocity = new Vec3(vx, vy, vz).scale(state.enchantment_core$getRicochetRetention());
            entity.setDeltaMovement(reflectedVelocity);

            // Recompute rotation matrix to prevent the projectile from continuing to visually point in the old direction.
            double horizontalDistance = reflectedVelocity.horizontalDistance();
            entity.setYRot((float) (Math.atan2(reflectedVelocity.x, reflectedVelocity.z) * (180.0 / Math.PI)));
            entity.setXRot((float) (Math.atan2(reflectedVelocity.y, horizontalDistance) * (180.0 / Math.PI)));
            entity.yRotO = entity.getYRot();
            entity.xRotO = entity.getXRot();

            // Flag to the engine that the entity requires an immediate network position update.
            entity.hasImpulse = true;

            // Halt the original routine so the arrow does not embed itself in the block.
            ci.cancel();
        }
    }
}