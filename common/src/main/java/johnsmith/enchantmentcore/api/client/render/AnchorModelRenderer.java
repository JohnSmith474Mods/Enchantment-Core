package johnsmith.enchantmentcore.api.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

/**
 * Contract for executing custom client-side model rendering logic for spell field anchor entities.
 * <p>
 * Implementations are registered through {@link AnchorRendererRegistry} and dispatched when
 * a spell field anchor specifies a corresponding model identifier.
 */
@FunctionalInterface
public interface AnchorModelRenderer {

    /**
     * Renders a custom model representation for the specified anchor entity.
     *
     * @param entity       The anchor entity instance being rendered.
     * @param entityYaw    The horizontal rotation angle of the entity in degrees.
     * @param partialTicks The normalized progression between the current and previous client tick.
     * @param poseStack    The active transformation matrix stack.
     * @param bufferSource The multi-buffer source providing vertex consumers.
     * @param packedLight  The combined block and sky light values at the entity's position.
     */
    void render(Entity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight);
}