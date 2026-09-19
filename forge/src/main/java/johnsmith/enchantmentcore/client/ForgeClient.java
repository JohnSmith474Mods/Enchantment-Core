package johnsmith.enchantmentcore.client;

import johnsmith.enchantmentcore.client.debug.SpellFieldDebugTracker;
import johnsmith.enchantmentcore.client.render.SpellFieldAnchorRenderer;
import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.registry.EnchantmentCoreEntities;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ForgeClient {

    /**
     * Executes strict client-side initialization, segregating visual APIs from the dedicated server.
     *
     * @param context  The Java FML mod loading context.
     * @param busGroup The primary Mod bus group required for non-static event mapping.
     */
    public static void initialize(FMLJavaModLoadingContext context, BusGroup busGroup) {
        // Explicitly bind client events to their localized and static buses
        FMLClientSetupEvent.getBus(busGroup).addListener(ForgeClient::onClientSetup);
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(ForgeClient::registerRenderers);
        TickEvent.ClientTickEvent.Post.BUS.addListener(ForgeClient::onClientTick);

        // Attach configuration UI
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parentScreen) -> Config.MANAGER.createScreen(parentScreen)
                )
        );
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(CommonClient::initialize);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EnchantmentCoreEntities.SPELL_FIELD_ANCHOR, SpellFieldAnchorRenderer::new);
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        SpellFieldDebugTracker.tick();
    }
}