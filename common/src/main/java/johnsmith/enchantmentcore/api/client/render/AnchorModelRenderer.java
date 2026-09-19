package johnsmith.enchantmentcore.api.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import johnsmith.enchantmentcore.client.render.SpellFieldAnchorRenderer.SpellFieldAnchorRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Contract for executing custom client-side model rendering logic for spell field anchor entities.
 * <p>
 * Implementations are registered through {@link AnchorRendererRegistry} and dispatched when
 * a spell field anchor specifies a corresponding model identifier.
 */
@FunctionalInterface
public interface AnchorModelRenderer {

    /**
     * Submits custom model geometry to the render graph for the specified anchor render state.
     *
     * @param state             The extracted render state data for the anchor entity.
     * @param poseStack         The active transformation matrix stack.
     * @param nodeCollector     The graph-based geometry collector for deferred rendering.
     * @param cameraRenderState Active camera state data, including packed light values and culling frustums.
     */
    void submit(SpellFieldAnchorRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState);
}