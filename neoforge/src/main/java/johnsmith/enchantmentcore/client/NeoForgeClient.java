package johnsmith.enchantmentcore.client;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.client.debug.SpellFieldDebugRenderer;
import johnsmith.enchantmentcore.client.debug.SpellFieldDebugTracker;
import johnsmith.enchantmentcore.client.render.SpellFieldAnchorRenderer;
import johnsmith.enchantmentcore.registry.EnchantmentCoreEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public class NeoForgeClient {

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(CommonClient::initialize);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(EnchantmentCoreEntities.SPELL_FIELD_ANCHOR, SpellFieldAnchorRenderer::new);
        }
    }

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static class GameEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            SpellFieldDebugTracker.tick();
        }

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                SpellFieldDebugRenderer.render(event.getPoseStack(), event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
            }
        }
    }
}