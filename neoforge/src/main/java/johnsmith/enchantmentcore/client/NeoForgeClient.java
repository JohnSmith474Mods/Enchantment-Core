package johnsmith.enchantmentcore.client;

import com.mojang.blaze3d.vertex.PoseStack;

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

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class NeoForgeClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(CommonClient::initialize);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EnchantmentCoreEntities.SPELL_FIELD_ANCHOR, SpellFieldAnchorRenderer::new);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        SpellFieldDebugTracker.tick();
    }
}