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
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class SpellFieldAnchorRenderer extends EntityRenderer<SpellFieldAnchorEntity, SpellFieldAnchorRenderer.SpellFieldAnchorRenderState> {

    public SpellFieldAnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SpellFieldAnchorRenderState createRenderState() {
        return new SpellFieldAnchorRenderState();
    }

    @Override
    public void extractRenderState(SpellFieldAnchorEntity entity, SpellFieldAnchorRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.modelIdStr = entity.getModelId();
        state.textureStr = entity.getVisualTexture();
        state.visualScale = entity.getVisualScale();
        state.visualFrames = entity.getVisualFrames();
        state.visualTickRate = entity.getVisualTickRate();
        state.visualTint = entity.getVisualTint();
        state.tickCount = entity.tickCount;
    }

    @Override
    public void render(SpellFieldAnchorRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (state.modelIdStr != null && !state.modelIdStr.isEmpty()) {
            AnchorModelRenderer delegate = AnchorRendererRegistry.get(ResourceLocation.parse(state.modelIdStr));
            if (delegate != null) {
                delegate.render(state, poseStack, bufferSource, packedLight);
                super.render(state, poseStack, bufferSource, packedLight);
                return;
            }
        }

        if (state.textureStr == null || state.textureStr.isEmpty()) {
            super.render(state, poseStack, bufferSource, packedLight);
            return;
        }

        ResourceLocation texture = ResourceLocation.parse(state.textureStr);
        float scale = state.visualScale;

        if (scale <= 0.01F) scale = 1.0F;

        int frames = Math.max(1, state.visualFrames);
        int frameRate = Math.max(1, state.visualTickRate);
        int tint = state.visualTint;

        int r = (tint >> 16) & 0xFF;
        int g = (tint >> 8) & 0xFF;
        int b = tint & 0xFF;

        poseStack.pushPose();
        poseStack.translate(0.0D, scale / 2.0D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(scale, scale, scale);

        int currentFrame = (state.tickCount / frameRate) % frames;
        float v0 = (float) currentFrame / frames;
        float v1 = (float) (currentFrame + 1) / frames;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.itemEntityTranslucentCull(texture));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();

        int light = 15728880;

        consumer.addVertex(matrix4f, -0.5F, -0.5F, 0.0F).setColor(r, g, b, 255).setUv(0.0F, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix4f,  0.5F, -0.5F, 0.0F).setColor(r, g, b, 255).setUv(1.0F, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix4f,  0.5F,  0.5F, 0.0F).setColor(r, g, b, 255).setUv(1.0F, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(matrix4f, -0.5F,  0.5F, 0.0F).setColor(r, g, b, 255).setUv(0.0F, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);

        consumer.addVertex(matrix4f, -0.5F,  0.5F, 0.0F).setColor(r, g, b, 255).setUv(0.0F, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, -1.0F, 0.0F);
        consumer.addVertex(matrix4f,  0.5F,  0.5F, 0.0F).setColor(r, g, b, 255).setUv(1.0F, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, -1.0F, 0.0F);
        consumer.addVertex(matrix4f,  0.5F, -0.5F, 0.0F).setColor(r, g, b, 255).setUv(1.0F, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, -1.0F, 0.0F);
        consumer.addVertex(matrix4f, -0.5F, -0.5F, 0.0F).setColor(r, g, b, 255).setUv(0.0F, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, -1.0F, 0.0F);

        poseStack.popPose();
        super.render(state, poseStack, bufferSource, packedLight);
    }

    @Override
    protected AABB getBoundingBoxForCulling(SpellFieldAnchorEntity entity) {
        return entity.getBoundingBox().inflate(entity.getVisualScale());
    }

    public static class SpellFieldAnchorRenderState extends EntityRenderState {
        public String modelIdStr;
        public String textureStr;
        public float visualScale;
        public int visualFrames;
        public int visualTickRate;
        public int visualTint;
        public int tickCount;
    }

}