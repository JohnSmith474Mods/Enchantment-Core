package johnsmith.enchantmentcore.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import johnsmith.enchantmentcore.client.debug.SpellFieldDebugRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects debug frustum rendering directly into the core debug rendering sequence.
 * Guarantees the GPU and PoseStack are correctly synchronized to the world-space camera matrix.
 */
@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void enchantment_core$renderDebugFrustum(
            PoseStack poseStack,
            Frustum frustum,
            MultiBufferSource.BufferSource bufferSource,
            double camX,
            double camY,
            double camZ,
            CallbackInfo ci
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        SpellFieldDebugRenderer.render(poseStack, minecraft.gameRenderer.getMainCamera(), partialTick);
    }
}