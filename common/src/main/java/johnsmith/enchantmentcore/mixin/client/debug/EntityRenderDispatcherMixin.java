package johnsmith.enchantmentcore.mixin.client.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.WeakHashMap;

import johnsmith.enchantmentcore.api.entity.accessor.ProjectileStateAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting the entity render dispatcher.
 * Injects diagnostic rendering logic to visualize projectile homing acquisition frustums.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Unique
    private static final WeakHashMap<EntityRenderState, FrustumRenderData> enchantment_core$frustumDataMap = new WeakHashMap<>();

    @Unique
    private record FrustumRenderData(
            double fovLimit,
            double minDist,
            double maxDist,
            float eyeHeight,
            float lerpedXRot,
            float lerpedYRot
    ) {}

    /**
     * Intercepts the entity extraction pipeline to capture rotation matrices and properties
     * before the entity reference is decoupled from the render state.
     */
    @Inject(
            method = "extractEntity",
            at = @At("RETURN")
    )
    private <E extends Entity> void enchantment_core$extractHomingFrustum(E entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        if (Minecraft.getInstance().debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES) && entity instanceof Projectile projectile) {
            ProjectileStateAccessor state = (ProjectileStateAccessor) projectile;
            double fovLimit = Math.min(179.0, state.enchantment_core$getHomingFov()) / 2.0;

            if (state.enchantment_core$getHomingStrength() <= 0.0 || fovLimit <= 0) return;

            float lerpedYRot = Mth.lerp(partialTick, projectile.yRotO, projectile.getYRot()) * ((float)Math.PI / 180F);
            float lerpedXRot = Mth.lerp(partialTick, projectile.xRotO, projectile.getXRot()) * ((float)Math.PI / 180F);

            enchantment_core$frustumDataMap.put(cir.getReturnValue(), new FrustumRenderData(
                    fovLimit,
                    Math.max(0.1, state.enchantment_core$getHomingMinDistance()),
                    state.enchantment_core$getHomingMaxDistance(),
                    projectile.getEyeHeight(),
                    lerpedXRot,
                    lerpedYRot
            ));
        }
    }

    /**
     * Intercepts the render submission pipeline.
     * Executes custom geometry submission for the homing frustum mapped to the active render state.
     */
    @Inject(
            method = "submit",
            at = @At("TAIL")
    )
    private <S extends EntityRenderState> void enchantment_core$submitHomingFrustum(S state, CameraRenderState cameraRenderState, double x, double y, double z, PoseStack poseStack, SubmitNodeCollector nodeCollector, CallbackInfo ci) {
        FrustumRenderData data = enchantment_core$frustumDataMap.get(state);
        if (data == null) return;

        poseStack.pushPose();
        poseStack.translate(x, y, z);

        // Convert spherical coordinates (pitch/yaw) into a normalized Cartesian direction vector.
        double dirX = Math.sin(data.lerpedYRot()) * Math.cos(data.lerpedXRot());
        double dirY = Math.sin(data.lerpedXRot());
        double dirZ = Math.cos(data.lerpedYRot()) * Math.cos(data.lerpedXRot());

        Vec3 forward = new Vec3(dirX, dirY, dirZ).normalize();

        // Establish the local coordinate frame (Forward, Right, Up) relative to the projectile orientation.
        Vec3 globalUp = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(globalUp);

        // Handle edge case where the projectile points exactly straight up or down.
        if (right.lengthSqr() < 1e-5) right = forward.cross(new Vec3(1, 0, 0));

        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        double tanFov = Math.tan(Math.toRadians(data.fovLimit()));
        Vec3 centerOffset = new Vec3(0, data.eyeHeight(), 0);

        // Calculate the lateral expansion of the frustum at the near and far clipping planes.
        double nearRadius = data.minDist() * tanFov;
        double farRadius = data.maxDist() * tanFov;

        Vec3[] nearCorners = new Vec3[4];
        Vec3[] farCorners = new Vec3[4];

        // Define quadrant multiplier combinations for the four corners of the frustum planes.
        int[][] signs = {{1, 1}, {1, -1}, {-1, -1}, {-1, 1}};

        // Compute the absolute spatial coordinates for the 8 vertices defining the frustum.
        for (int i = 0; i < 4; i++) {
            nearCorners[i] = centerOffset.add(forward.scale(data.minDist()))
                    .add(right.scale(signs[i][0] * nearRadius))
                    .add(up.scale(signs[i][1] * nearRadius));

            farCorners[i] = centerOffset.add(forward.scale(data.maxDist()))
                    .add(right.scale(signs[i][0] * farRadius))
                    .add(up.scale(signs[i][1] * farRadius));
        }

        // Submits directly to the asynchronous pipeline utilizing the CustomGeometryRenderer lambda hook.
        nodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, builder) -> {
            for (int i = 0; i < 4; i++) {
                int next = (i + 1) % 4;

                // Draw near plane perimeter.
                enchantment_core$drawLine(builder, pose, nearCorners[i], nearCorners[next], 255, 150, 0);
                // Draw far plane perimeter.
                enchantment_core$drawLine(builder, pose, farCorners[i], farCorners[next], 255, 200, 0);
                // Draw connecting edges between near and far planes.
                enchantment_core$drawLine(builder, pose, nearCorners[i], farCorners[i], 255, 150, 0);
            }
        });

        poseStack.popPose();
    }

    /**
     * Submits vertex data to draw a single colored line segment.
     */
    @Unique
    private void enchantment_core$drawLine(VertexConsumer builder, PoseStack.Pose pose, Vec3 p1, Vec3 p2, int r, int g, int b) {
        Matrix4f matrix = pose.pose();
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