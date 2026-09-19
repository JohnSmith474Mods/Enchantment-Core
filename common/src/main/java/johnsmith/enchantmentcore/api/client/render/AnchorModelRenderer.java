package johnsmith.enchantmentcore.api.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import johnsmith.enchantmentcore.client.render.SpellFieldAnchorRenderer.SpellFieldAnchorRenderState;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Contract for executing custom client-side model rendering logic for spell field anchor entities.
 * <p>
 * Implementations are registered through {@link AnchorRendererRegistry} and dispatched when
 * a spell field anchor specifies a corresponding model identifier.
 */
@FunctionalInterface
public interface AnchorModelRenderer {

    /**
     * Renders a custom model representation for the specified anchor render state.
     *
     * @param state        The extracted render state data for the anchor entity.
     * @param poseStack    The active transformation matrix stack.
     * @param bufferSource The multi-buffer source providing vertex consumers.
     * @param packedLight  The combined block and sky light values at the entity's position.
     */
    void render(SpellFieldAnchorRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight);
}