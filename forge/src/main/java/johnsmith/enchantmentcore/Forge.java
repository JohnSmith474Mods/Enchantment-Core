package johnsmith.enchantmentcore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import johnsmith.enchantmentcore.api.config.PackInclusionType;
import johnsmith.enchantmentcore.command.ImportCommand;
import johnsmith.enchantmentcore.command.ExportCommand;
import johnsmith.enchantmentcore.command.SpellFieldCommand;
import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.platform.ForgeRegistryHelper;
import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.registry.EnchantmentValueRegistry;
import johnsmith.enchantmentcore.registry.EnchantmentCoreEntities;
import johnsmith.enchantmentcore.registry.EnchantmentCoreRegistries;
import johnsmith.enchantmentcore.registry.SpellFieldRegistrar;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegisterEvent;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;

@Mod(Constants.MOD_ID)
public class Forge {
    public Forge(FMLJavaModLoadingContext context) {
        IEventBus eventBus = context.getModEventBus();

        // Initialize common classes
        Common.initialize();

        // Resolve Config Path
        Config.MANAGER.init(FMLPaths.CONFIGDIR.get());

        // Register Library Components
        EnchantmentEffectComponentRegistry.initialize();
        ForgeRegistryHelper.registerAll(eventBus);
        eventBus.addListener(this::onRegister);
        eventBus.addListener(this::onNewRegistry);
        eventBus.addListener(this::onRegisterRenderers);


        // Register Optional Data
        eventBus.addListener(this::onAddPackFinders);

        // Register Commands
        EVENT_BUS.addListener(this::onRegisterCommands);

        // Attach Screen
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (minecraft, parentScreen) -> Config.MANAGER.createScreen(parentScreen)
                    )
            );
        }
    }

    private void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(EnchantmentCoreRegistries.DISTANCE_SCALING_FUNCTION_KEY)) {
            SpellFieldRegistrar.registerFunctions((id, codec) ->
                    event.register(EnchantmentCoreRegistries.DISTANCE_SCALING_FUNCTION_KEY, id, () -> codec));
        }
        if (event.getRegistryKey().equals(EnchantmentCoreRegistries.SPELL_FIELD_SHAPE_KEY)) {
            SpellFieldRegistrar.registerShapes((id, codec) ->
                    event.register(EnchantmentCoreRegistries.SPELL_FIELD_SHAPE_KEY, id, () -> codec));
        }
        if (event.getRegistryKey().equals(EnchantmentCoreRegistries.SPELL_FIELD_EFFECT_KEY)) {
            SpellFieldRegistrar.registerEffects((id, codec) ->
                    event.register(EnchantmentCoreRegistries.SPELL_FIELD_EFFECT_KEY, id, () -> codec));
        }
        if (event.getRegistryKey().equals(BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE.key())) {
            EnchantmentValueRegistry.initialize();
        }
        if (event.getRegistryKey().equals(BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE.key())) {
            EnchantmentEffectComponentRegistry.initialize();
        }
        if (event.getRegistryKey().equals(BuiltInRegistries.ENTITY_TYPE.key())) {
            EnchantmentCoreEntities.initialize();
        }
    }

    private void onNewRegistry(NewRegistryEvent event) {
        for (net.minecraftforge.registries.RegistryBuilder<?> builder : ForgeRegistryHelper.PENDING_BUILDERS) {
            event.create(builder);
        }
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EnchantmentCoreEntities.SPELL_FIELD_ANCHOR, NoopRenderer::new);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ImportCommand.register(event.getDispatcher());
        ExportCommand.register(event.getDispatcher());
        SpellFieldCommand.register(event.getDispatcher());
    }

    private void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            this.registerBuiltInPack(event, Constants.MOD_ID, "test_enchantments", "Enchantment Core Test Enchantments", PackInclusionType.OPTIONAL);
            this.registerBuiltInPack(event, "minecraft", "default_enchantments", "Configurable Default Enchantments", Config.CONFIGURABLE_DEFAULT_ENCHANTMENTS.get());
        }
    }

    private void registerBuiltInPack(AddPackFindersEvent event, String namespace, String directory, String name, PackInclusionType inclusionType) {
        Path path = ModList.get().getModFileById(Constants.MOD_ID).getFile().findResource("resourcepacks", directory);
        if (Files.exists(path)) {
            PackSelectionConfig selectionConfig = switch (inclusionType) {
                case REQUIRED -> new PackSelectionConfig(true, Pack.Position.TOP, true);
                case ACTIVE -> new PackSelectionConfig(true, Pack.Position.TOP, false);
                case OPTIONAL -> new PackSelectionConfig(false, Pack.Position.TOP, false);
            };

            Pack.ResourcesSupplier resourcesSupplier = new PathPackResources.PathResourcesSupplier(path);
            PackLocationInfo info = new PackLocationInfo(namespace + ":" + directory, Component.literal(name), PackSource.BUILT_IN, Optional.empty());
            Pack pack = Pack.readMetaAndCreate(info, resourcesSupplier, PackType.SERVER_DATA, selectionConfig);

            if (pack != null) {
                event.addRepositorySource(consumer -> consumer.accept(pack));
            }
        }
    }
}
