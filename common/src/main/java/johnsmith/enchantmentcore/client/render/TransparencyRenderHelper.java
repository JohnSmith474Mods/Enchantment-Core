package johnsmith.enchantmentcore.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;

import johnsmith.enchantmentcore.enchantment.effect.TransparencyEffect;
import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HitboxesRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Provides mechanisms to calculate and apply dynamic alpha transparency across asynchronous rendering pipelines.
 * Utilizes thread-local storage to maintain state during render pipeline construction.
 */
public class TransparencyRenderHelper {
    /**
     * Thread-local storage for the active alpha multiplier. Default value is 1.0F.
     */
    private static final ThreadLocal<Float> CURRENT_ALPHA = ThreadLocal.withInitial(() -> 1.0F);
    private static final ResourceLocation BLOCKS_ATLAS = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");

    /**
     * Sets the active alpha multiplier for the current thread.
     *
     * @param alpha The alpha multiplier to apply to subsequent render operations.
     */
    public static void setAlphaActive(float alpha) {
        CURRENT_ALPHA.set(alpha);
    }

    /**
     * Resets the active alpha multiplier to 1.0F for the current thread.
     */
    public static void clearAlphaActive() {
        CURRENT_ALPHA.set(1.0F);
    }

    /**
     * Evaluates if the active alpha multiplier dictates transparency.
     *
     * @return True if the active alpha is less than 1.0F. False otherwise.
     */
    public static boolean isAlphaActive() {
        return CURRENT_ALPHA.get() < 1.0F;
    }

    /**
     * Retrieves the active alpha multiplier for the current thread.
     *
     * @return The active alpha multiplier.
     */
    public static float getAlpha() {
        return CURRENT_ALPHA.get();
    }

    /**
     * Calculates the minimum alpha multiplier applicable to the specified item stack.
     * Iterates through applied enchantments and extracts values from transparency effects.
     *
     * @param stack The item stack to evaluate.
     * @return The calculated alpha multiplier, clamped between 0.0F and 1.0F.
     */
    public static float calculateAlpha(ItemStack stack) {
        if (stack.isEmpty()) return 1.0F;

        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return 1.0F;

        float minAlpha = 1.0F;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            List<ConditionalEffect<TransparencyEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.TRANSPARENCY.get());
            if (effects != null) {
                for (ConditionalEffect<TransparencyEffect> cond : effects) {
                    minAlpha = Math.min(minAlpha, cond.effect().alphaMultiplier().calculate(entry.getIntValue()));
                }
            }
        }

        return Math.max(0.0F, Math.min(1.0F, minAlpha));
    }

    /**
     * Wraps a node collector to intercept rendering submissions.
     * Applies the specified alpha multiplier to packed ARGB color integers and custom vertex geometries.
     *
     * @param original The vanilla node collector.
     * @param alpha    The alpha multiplier to apply.
     * @return A proxy node collector that applies the alpha modifier.
     */
    public static SubmitNodeCollector wrapCollector(SubmitNodeCollector original, float alpha) {
        return new AlphaNodeCollector(original, original, alpha);
    }

    /**
     * Proxy implementation of SubmitNodeCollector.
     * Intercepts render submissions, modifies color parameters, aborts submissions below a
     * visual threshold, and delegates to the root collector.
     */
    private record AlphaNodeCollector(
            OrderedSubmitNodeCollector delegate,
            SubmitNodeCollector root,
            float alpha
    ) implements SubmitNodeCollector {

        @Override
        public OrderedSubmitNodeCollector order(int index) {
            return new AlphaNodeCollector(this.root.order(index), this.root, this.alpha);
        }

        @Override
        public void submitHitbox(
                PoseStack poseStack,
                EntityRenderState state,
                HitboxesRenderState hitboxes
        ) {
            if (this.alpha <= 0.00004F) return;
            this.delegate.submitHitbox(poseStack, state, hitboxes);
        }

        @Override
        public void submitShadow(
                PoseStack poseStack,
                float radius,
                List<EntityRenderState.ShadowPiece> pieces
        ) {
            if (this.alpha <= 0.00004F) return;
            this.delegate.submitShadow(poseStack, radius, pieces);
        }

        @Override
        public void submitNameTag(
                PoseStack poseStack,
                Vec3 offset, int yOffset,
                Component text,
                boolean isDiscrete,
                int light,
                double distanceSq,
                CameraRenderState cameraState
        ) {
            if (this.alpha <= 0.00004F) return;
            this.delegate.submitNameTag(poseStack, offset, yOffset, text, isDiscrete, light, distanceSq, cameraState);
        }

        @Override
        public void submitText(
                PoseStack poseStack,
                float x,
                float y,
                FormattedCharSequence text,
                boolean dropShadow,
                Font.DisplayMode mode,
                int color,
                int light,
                int overlay,
                int outlineColor
        ) {
            if (this.alpha <= 0.00004F) return;
            int modifiedColor = applyAlpha(color, this.alpha);
            int modifiedOutline = applyAlpha(outlineColor, this.alpha);
            this.delegate.submitText(poseStack, x, y, text, dropShadow, mode, modifiedColor, light, overlay, modifiedOutline);
        }

        @Override
        public void submitFlame(
                PoseStack poseStack,
                EntityRenderState state,
                Quaternionf rotation
        ) {
            if (this.alpha <= 0.00004F) return;
            this.delegate.submitFlame(poseStack, state, rotation);
        }

        @Override
        public void submitLeash(
                PoseStack poseStack,
                EntityRenderState.LeashState state
        ) {
            if (this.alpha <= 0.00004F) return;
            this.delegate.submitLeash(poseStack, state);
        }

        /**
         * Intercepts model submissions for entities/armor to dynamically shift their tint color alpha.
         */
        @Override
        public <S> void submitModel(
                Model<? super S> model, S state,
                PoseStack poseStack,
                RenderType renderType,
                int light,
                int overlay,
                int color,
                TextureAtlasSprite sprite,
                int outlineColor,
                ModelFeatureRenderer.CrumblingOverlay crumbling
        ) {
            if (this.alpha <= 0.00004F) return;
            int modifiedColor = applyAlpha(color, this.alpha);
            int modifiedOutline = applyAlpha(outlineColor, this.alpha);
            this.delegate.submitModel(model, state, poseStack, renderType, light, overlay, modifiedColor, sprite, modifiedOutline, crumbling);
        }

        /**
         * Intercepts isolated model part submissions to dynamically shift their tint color alpha.
         */
        @Override
        public void submitModelPart(
                ModelPart part,
                PoseStack poseStack,
                RenderType renderType,
                int light,
                int overlay,
                TextureAtlasSprite sprite,
                boolean hasFoil,
                boolean isDiscrete,
                int color,
                ModelFeatureRenderer.CrumblingOverlay crumbling,
                int outlineColor
        ) {
            if (this.alpha <= 0.00004F) return;
            int modifiedColor = applyAlpha(color, this.alpha);
            int modifiedOutline = applyAlpha(outlineColor, this.alpha);
            this.delegate.submitModelPart(part, poseStack, renderType, light, overlay, sprite, hasFoil, isDiscrete, modifiedColor, crumbling, modifiedOutline);
        }

        @Override
        public void submitBlock(
                PoseStack poseStack,
                BlockState state,
                int x,
                int y,
                int z
        ) {
            if (this.alpha <= 0.00004F) return;
            this.delegate.submitBlock(poseStack, state, x, y, z);
        }

        @Override
        public void submitMovingBlock(
                PoseStack poseStack,
                MovingBlockRenderState state
        ) {
            if (this.alpha <= 0.00004F) return;
            this.delegate.submitMovingBlock(poseStack, state);
        }

        @Override
        public void submitBlockModel(
                PoseStack poseStack,
                RenderType renderType,
                BlockStateModel model,
                float r,
                float g,
                float b,
                int color,
                int light,
                int overlay
        ) {
            if (this.alpha <= 0.00004F) return;
            RenderType activeType = RenderType.entityTranslucent(BLOCKS_ATLAS);
            int modifiedColor = applyAlpha(color, this.alpha);
            this.delegate.submitBlockModel(poseStack, activeType, model, r, g, b, modifiedColor, light, overlay);
        }

        /**
         * Intercepts item model submissions to unpack standard BakedQuad definitions.
         * Enforces backface culling properly within UI spaces and modifies static vertex alpha arrays.
         */
        @Override
        public void submitItem(
                PoseStack poseStack,
                ItemDisplayContext context,
                int light,
                int overlay,
                int outlineColor,
                int[] tintLayers,
                List<BakedQuad> quads,
                RenderType renderType,
                ItemStackRenderState.FoilType foilType
        ) {
            float clampedAlpha = Math.max(0.0F, Math.min(1.0F, this.alpha));

            if (clampedAlpha <= 0.00004F) {
                return;
            }

            if (clampedAlpha >= 1.0F) {
                this.delegate.submitItem(poseStack, context, light, overlay, outlineColor, tintLayers, quads, renderType, foilType);
                return;
            }

            RenderType translucentType = RenderType.entityTranslucent(BLOCKS_ATLAS);
            int[] modifiedTints = new int[tintLayers.length];

            for (int i = 0; i < tintLayers.length; i++) {
                modifiedTints[i] = applyAlpha(tintLayers[i] == -1 ? 0xFFFFFFFF : tintLayers[i], this.alpha);
            }

            this.delegate.submitCustomGeometry(poseStack, translucentType, (pose, consumer) -> {
                for (BakedQuad quad : quads) {
                    int tint = 0xFFFFFFFF;
                    if (quad.tintIndex() != -1 && quad.tintIndex() < tintLayers.length) {
                        int layerTint = tintLayers[quad.tintIndex()];
                        if (layerTint != -1) {
                            tint = layerTint;
                        }
                    }

                    float r = ((tint >> 16) & 0xFF) / 255.0F;
                    float g = ((tint >> 8) & 0xFF) / 255.0F;
                    float b = (tint & 0xFF) / 255.0F;
                    float a = (((tint >> 24) & 0xFF) / 255.0F) * clampedAlpha;

                    consumer.putBulkData(pose, quad, r, g, b, a, light, overlay);
                }
            });

            if (foilType != ItemStackRenderState.FoilType.NONE) {
                RenderType glintType = foilType == ItemStackRenderState.FoilType.SPECIAL ? RenderType.glintTranslucent() : RenderType.glint();
                this.delegate.submitCustomGeometry(poseStack, glintType, (pose, consumer) -> {
                    for (BakedQuad quad : quads) {
                        consumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, clampedAlpha, light, overlay);
                    }
                });
            }
        }

        /**
         * Wraps custom geometry pipelines to intercept and adjust the manual vertex color emissions.
         */
        @Override
        public void submitCustomGeometry(
                PoseStack poseStack,
                RenderType renderType,
                CustomGeometryRenderer renderer
        ) {
            if (this.alpha <= 0.00004F) return;
            this.delegate.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
                VertexConsumer wrappedConsumer = new VertexConsumer() {
                    @Override
                    public VertexConsumer addVertex(
                            float x,
                            float y,
                            float z
                    ) {
                        consumer.addVertex(x, y, z);
                        return this;
                    }

                    @Override
                    public VertexConsumer setColor(
                            int r,
                            int g,
                            int b,
                            int a
                    ) {
                        consumer.setColor(r, g, b, (int) (a * alpha));
                        return this;
                    }

                    @Override
                    public VertexConsumer setUv(
                            float u,
                            float v
                    ) {
                        consumer.setUv(u, v);
                        return this;
                    }

                    @Override
                    public VertexConsumer setUv1(
                            int u,
                            int v
                    ) {
                        consumer.setUv1(u, v);
                        return this;
                    }

                    @Override
                    public VertexConsumer setUv2(
                            int u,
                            int v
                    ) {
                        consumer.setUv2(u, v);
                        return this;
                    }

                    @Override
                    public VertexConsumer setNormal(
                            float x,
                            float y,
                            float z
                    ) {
                        consumer.setNormal(x, y, z);
                        return this;
                    }
                };
                renderer.render(pose, wrappedConsumer);
            });
        }

        @Override
        public void submitParticleGroup(
                ParticleGroupRenderer renderer
        ) {
            if (this.alpha <= 0.00004F) return;
            this.delegate.submitParticleGroup(renderer);
        }

        /**
         * Applies the target alpha multiplier to a packed ARGB integer.
         *
         * @param packedColor The original packed ARGB color integer.
         * @param multiplier  The alpha multiplier to apply.
         * @return The modified packed ARGB color integer.
         */
        private static int applyAlpha(
                int packedColor,
                float multiplier
        ) {
            int a = (int) (((packedColor >> 24) & 0xFF) * multiplier);
            return (packedColor & 0x00FFFFFF) | (a << 24);
        }
    }
}