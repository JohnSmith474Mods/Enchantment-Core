package johnsmith.enchantmentcore.mixin.client.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import johnsmith.enchantmentcore.api.entity.accessor.ProjectileStateAccessor;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting the entity render dispatcher.
 * Injects diagnostic rendering logic to visualize projectile homing acquisition frustums.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Shadow public abstract boolean shouldRenderHitBoxes();

    /**
     * Intercepts the standard entity rendering pipeline.
     * Executes custom rendering for the homing frustum if the entity is a configured projectile and hitbox rendering is active.
     *
     * @param entity       The entity currently being rendered.
     * @param x            The interpolated X coordinate.
     * @param y            The interpolated Y coordinate.
     * @param z            The interpolated Z coordinate.
     * @param partialTick  The fractional tick value for interpolation.
     * @param poseStack    The active matrix stack.
     * @param bufferSource The active buffer source.
     * @param packedLight  The calculated light level.
     * @param ci           The callback information.
     */
    @Inject(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL")
    )
    private <E extends Entity> void enchantment_core$renderHomingFrustum(E entity, double x, double y, double z, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {

        if (this.shouldRenderHitBoxes() && entity instanceof Projectile projectile) {
            ProjectileStateAccessor state = (ProjectileStateAccessor) projectile;
            double fovLimit = Math.min(179.0, state.enchantment_core$getHomingFov()) / 2.0;

            if (state.enchantment_core$getHomingStrength() <= 0.0 || fovLimit <= 0) return;

            poseStack.pushPose();
            poseStack.translate(x, y, z);

            VertexConsumer builder = bufferSource.getBuffer(RenderType.lines());
            PoseStack.Pose currentPose = poseStack.last();
            Matrix4f matrix = currentPose.pose();

            // Interpolate pitch and yaw across the current tick to prevent visual stuttering.
            float lerpedYRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) * ((float)Math.PI / 180F);
            float lerpedXRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) * ((float)Math.PI / 180F);

            // Convert spherical coordinates (pitch/yaw) into a normalized Cartesian direction vector.
            double dirX = Math.sin(lerpedYRot) * Math.cos(lerpedXRot);
            double dirY = Math.sin(lerpedXRot);
            double dirZ = Math.cos(lerpedYRot) * Math.cos(lerpedXRot);

            Vec3 forward = new Vec3(dirX, dirY, dirZ).normalize();

            // Establish the local coordinate frame (Forward, Right, Up) relative to the projectile orientation.
            Vec3 globalUp = new Vec3(0, 1, 0);
            Vec3 right = forward.cross(globalUp);
            // Handle edge case where the projectile points exactly straight up or down.
            if (right.lengthSqr() < 1e-5) right = forward.cross(new Vec3(1, 0, 0));
            right = right.normalize();
            Vec3 up = right.cross(forward).normalize();

            double minDist = Math.max(0.1, state.enchantment_core$getHomingMinDistance());
            double maxDist = state.enchantment_core$getHomingMaxDistance();
            double tanFov = Math.tan(Math.toRadians(fovLimit));

            Vec3 centerOffset = new Vec3(0, entity.getEyeHeight(), 0);

            // Calculate the lateral expansion of the frustum at the near and far clipping planes.
            double nearRadius = minDist * tanFov;
            double farRadius = maxDist * tanFov;

            Vec3[] nearCorners = new Vec3[4];
            Vec3[] farCorners = new Vec3[4];

            // Define quadrant multiplier combinations for the four corners of the frustum planes.
            int[][] signs = {{1, 1}, {1, -1}, {-1, -1}, {-1, 1}};

            // Compute the absolute spatial coordinates for the 8 vertices defining the frustum.
            for (int i = 0; i < 4; i++) {
                nearCorners[i] = centerOffset.add(forward.scale(minDist))
                        .add(right.scale(signs[i][0] * nearRadius))
                        .add(up.scale(signs[i][1] * nearRadius));

                farCorners[i] = centerOffset.add(forward.scale(maxDist))
                        .add(right.scale(signs[i][0] * farRadius))
                        .add(up.scale(signs[i][1] * farRadius));
            }

            // Execute line rendering between the calculated vertices to construct the wireframe.
            for (int i = 0; i < 4; i++) {
                int next = (i + 1) % 4;

                // Draw near plane perimeter.
                enchantment_core$drawLine(builder, matrix, currentPose, nearCorners[i], nearCorners[next], 255, 150, 0);
                // Draw far plane perimeter.
                enchantment_core$drawLine(builder, matrix, currentPose, farCorners[i], farCorners[next], 255, 200, 0);
                // Draw connecting edges between near and far planes.
                enchantment_core$drawLine(builder, matrix, currentPose, nearCorners[i], farCorners[i], 255, 150, 0);
            }

            poseStack.popPose();
        }
    }

    /**
     * Submits vertex data to draw a single colored line segment.
     *
     * @param builder The vertex consumer.
     * @param matrix  The transformation matrix.
     * @param pose    The pose state.
     * @param p1      The starting coordinate.
     * @param p2      The ending coordinate.
     * @param r       The red color component (0-255).
     * @param g       The green color component (0-255).
     * @param b       The blue color component (0-255).
     */
    @Unique
    private void enchantment_core$drawLine(VertexConsumer builder, Matrix4f matrix, PoseStack.Pose pose, Vec3 p1, Vec3 p2, int r, int g, int b) {
        float nx = (float) (p2.x - p1.x);
        float ny = (float) (p2.y - p1.y);
        float nz = (float) (p2.z - p1.z);

        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);

        if (len > 1.0E-5F) {
            nx /= len;
            ny /= len;
            nz /= len;
        } else {
            nx = 0.0f;
            ny = 1.0f;
            nz = 0.0f;
        }

        builder.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, 255).setNormal(pose, nx, ny, nz);
        builder.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, 255).setNormal(pose, nx, ny, nz);
    }
}