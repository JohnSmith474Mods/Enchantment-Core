package johnsmith.enchantmentcore.mixin.client.render;

import java.util.List;

import johnsmith.enchantmentcore.client.debug.SpellFieldDebugRenderer;

import net.minecraft.client.renderer.debug.DebugRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modifies the debug renderer initialization sequence.
 * Appends the custom spell field visualizer to the native rendering execution loop.
 */
@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {

    /**
     * The native list of active debug renderers.
     */
    @Shadow
    private List<DebugRenderer.SimpleDebugRenderer> renderers;

    /**
     * Injects at the end of the renderer refresh cycle.
     * Instantiates and registers the SpellFieldDebugRenderer.
     *
     * @param ci The callback information object.
     */
    @Inject(method = "refreshRendererList", at = @At("TAIL"))
    private void enchantment_core$registerDebugRenderer(CallbackInfo ci) {
        this.renderers.add(new SpellFieldDebugRenderer());
    }
}