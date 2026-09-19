package johnsmith.enchantmentcore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import johnsmith.enchantmentcore.client.debug.SpellFieldDebugRenderer;
import johnsmith.enchantmentcore.client.debug.SpellFieldDebugTracker;
import johnsmith.enchantmentcore.client.render.SpellFieldAnchorRenderer;
import johnsmith.enchantmentcore.registry.EnchantmentCoreEntities;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class FabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CommonClient.initialize();
        EntityRendererRegistry.register(EnchantmentCoreEntities.SPELL_FIELD_ANCHOR, SpellFieldAnchorRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> SpellFieldDebugTracker.tick());

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            PoseStack poseStack = new PoseStack();
            SpellFieldDebugRenderer.render(poseStack, context.camera(), context.tickCounter().getGameTimeDeltaPartialTick(true));
        });
    }
}