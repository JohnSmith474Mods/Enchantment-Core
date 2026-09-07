package johnsmith.enchantmentcore.client;

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

import org.joml.Matrix4f;

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
        @SuppressWarnings("deprecation")
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                // Extract game-time simulation delta to align entity interpolation with camera motion.
                float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);

                // The event.getPoseStack() method returns the compound Frustum matrix (Projection * ModelView = Compound).
                Matrix4f frustumMatrix = event.getPoseStack();

                // Invert the projection matrix.
                Matrix4f projectionInverse = new Matrix4f(event.getProjectionMatrix()).invert();

                // Multiply the inverted projection matrix by the compound Frustum matrix (Compound * Inverse = ModelView).
                Matrix4f modelViewMatrix = projectionInverse.mul(frustumMatrix);

                SpellFieldDebugRenderer.render(modelViewMatrix, event.getCamera(), partialTick);
            }
        }
    }
}