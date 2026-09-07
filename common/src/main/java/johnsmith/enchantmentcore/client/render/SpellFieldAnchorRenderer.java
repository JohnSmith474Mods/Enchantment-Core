package johnsmith.enchantmentcore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import johnsmith.enchantmentcore.api.client.render.AnchorModelRenderer;
import johnsmith.enchantmentcore.api.client.render.AnchorRendererRegistry;
import johnsmith.enchantmentcore.enchantment.spellfield.entity.SpellFieldAnchorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class SpellFieldAnchorRenderer extends EntityRenderer<SpellFieldAnchorEntity> {

    public SpellFieldAnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SpellFieldAnchorEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        String modelIdStr = entity.getModelId();
        if (modelIdStr != null && !modelIdStr.isEmpty()) {
            AnchorModelRenderer delegate = AnchorRendererRegistry.get(ResourceLocation.parse(modelIdStr));
            if (delegate != null) {
                delegate.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
                super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
                return;
            }
        }

        String textureStr = entity.getVisualTexture();
        if (textureStr == null || textureStr.isEmpty()) {
            super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
            return;
        }

        ResourceLocation texture = ResourceLocation.parse(textureStr);
        float scale = entity.getVisualScale();

        // 1. Failsafe against delayed data sync
        if (scale <= 0.01F) scale = 1.0F;

        int frames = Math.max(1, entity.getVisualFrames());
        int frameRate = Math.max(1, entity.getVisualTickRate());
        int tint = entity.getVisualTint();

        int r = (tint >> 16) & 0xFF;
        int g = (tint >> 8) & 0xFF;
        int b = tint & 0xFF;

        poseStack.pushPose();
        poseStack.translate(0.0D, scale / 2.0D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(scale, scale, scale);

        int currentFrame = (entity.tickCount / frameRate) % frames;
        float v0 = (float) currentFrame / frames;
        float v1 = (float) (currentFrame + 1) / frames;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentCull(texture));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();

        // 2. Force full brightness (LightTexture.FULL_BRIGHT) for spell effects
        // to prevent them from rendering black inside entity hitboxes or blocks.
        int light = 15728880;

        // 3. Front Face: Counter-Clockwise (CCW) Winding.
        consumer.addVertex(matrix4f, -0.5F, -0.5F, 0.0F).setColor(r, g, b, 255).setUv(0.0F, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix4f,  0.5F, -0.5F, 0.0F).setColor(r, g, b, 255).setUv(1.0F, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix4f,  0.5F,  0.5F, 0.0F).setColor(r, g, b, 255).setUv(1.0F, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix4f, -0.5F,  0.5F, 0.0F).setColor(r, g, b, 255).setUv(0.0F, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);

        // 4. Back Face: Clockwise (CW) Winding.
        // Eliminates rendering failure entirely by explicitly drawing the reverse side of the quad.
        consumer.addVertex(matrix4f, -0.5F,  0.5F, 0.0F).setColor(r, g, b, 255).setUv(0.0F, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, -1.0F, 0.0F);
        consumer.addVertex(matrix4f,  0.5F,  0.5F, 0.0F).setColor(r, g, b, 255).setUv(1.0F, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, -1.0F, 0.0F);
        consumer.addVertex(matrix4f,  0.5F, -0.5F, 0.0F).setColor(r, g, b, 255).setUv(1.0F, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, -1.0F, 0.0F);
        consumer.addVertex(matrix4f, -0.5F, -0.5F, 0.0F).setColor(r, g, b, 255).setUv(0.0F, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, -1.0F, 0.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SpellFieldAnchorEntity entity) {
        String textureStr = entity.getVisualTexture();
        return (textureStr != null && !textureStr.isEmpty()) ? ResourceLocation.parse(textureStr) : null;
    }
}