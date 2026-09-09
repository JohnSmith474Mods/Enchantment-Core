package johnsmith.enchantmentcore.client;

import com.mojang.blaze3d.vertex.PoseStack;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.client.debug.SpellFieldDebugRenderer;
import johnsmith.enchantmentcore.client.debug.SpellFieldDebugTracker;
import johnsmith.enchantmentcore.client.render.SpellFieldAnchorRenderer;
import johnsmith.enchantmentcore.registry.EnchantmentCoreEntities;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ForgeClient {

    @Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
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

    @Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                SpellFieldDebugTracker.tick();
            }
        }

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);

                PoseStack poseStack = new PoseStack();

                SpellFieldDebugRenderer.render(poseStack, event.getCamera(), partialTick);
            }
        }
    }
}