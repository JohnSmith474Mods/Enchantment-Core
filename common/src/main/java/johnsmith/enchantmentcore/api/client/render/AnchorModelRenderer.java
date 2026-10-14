package johnsmith.enchantmentcore.api.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import johnsmith.enchantmentcore.api.enchantment.spellfield.entity.SpellFieldAnchorEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

/**
 * Functional contract for executing custom client-side 3D model rendering logic for
 * {@link SpellFieldAnchorEntity} instances.
 * <p>
 * Implementations are registered through {@link AnchorRendererRegistry} and invoked during the entity
 * rendering pass when a spell field anchor specifies a matching {@code model_id} in its visual configuration.
 */
@FunctionalInterface
public interface AnchorModelRenderer {
    /**
     * Renders a custom model representation for the specified spell field anchor entity.
     *
     * @param entity       The anchor entity instance being rendered.
     * @param entityYaw    The horizontal rotation angle (yaw) of the entity in degrees.
     * @param partialTicks The normalized partial tick progression between the previous and current client tick.
     * @param poseStack    The active transformation matrix stack positioned at the entity's render origin.
     * @param bufferSource The multi-buffer source providing vertex consumers for render types.
     * @param packedLight  The combined block and sky light coordinates at the entity's position.
     */
    void render(Entity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight);
}