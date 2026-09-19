package johnsmith.enchantmentcore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import johnsmith.enchantmentcore.api.config.PackInclusionType;
import johnsmith.enchantmentcore.client.ForgeClient;
import johnsmith.enchantmentcore.command.ImportCommand;
import johnsmith.enchantmentcore.command.ExportCommand;
import johnsmith.enchantmentcore.command.SpellFieldCommand;
import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.event.ForgeGameEvents;
import johnsmith.enchantmentcore.platform.ForgeRegistryHelper;
import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.registry.EnchantmentValueRegistry;
import johnsmith.enchantmentcore.registry.EnchantmentCoreEntities;
import johnsmith.enchantmentcore.registry.EnchantmentCoreRegistries;
import johnsmith.enchantmentcore.registry.SpellFieldRegistrar;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;

@Mod(Constants.MOD_ID)
public class Forge {
    public Forge(FMLJavaModLoadingContext context) {
        BusGroup modBusGroup = context.getModBusGroup();

        // Initialize common classes
        Common.initialize();

        // Resolve Config Path
        Config.MANAGER.init(FMLPaths.CONFIGDIR.get());

        // Unfreeze purely data-driven vanilla registries. Forge does not fire RegisterEvent for these.
        unfreeze(BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE);
        unfreeze(BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE);
        unfreeze(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE);

        // Safely evaluate common static initializations into the unfrozen data registries.
        EnchantmentEffectComponentRegistry.initialize();
        EnchantmentValueRegistry.initialize();

        // Register Library Components
        ForgeRegistryHelper.registerAll(modBusGroup);
        RegisterEvent.getBus(modBusGroup).addListener(this::onRegister);
        NewRegistryEvent.BUS.addListener(this::onNewRegistry);

        // Register Optional Data
        AddPackFindersEvent.BUS.addListener(this::onAddPackFinders);

        // Register Commands
        RegisterCommandsEvent.BUS.addListener(this::onRegisterCommands);

        // Explicitly bind game events bypassing the AutomaticEventSubscriber
        ForgeGameEvents.initialize();

        // Isolate all Client boundary interactions
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClient.initialize(context, modBusGroup);
        }
    }

    /**
     * Bypasses the strict Forge registry lock for non-Forge-tracked registries.
     */
    private static void unfreeze(net.minecraft.core.Registry<?> registry) {
        if (registry instanceof net.minecraft.core.MappedRegistry<?> mappedRegistry) {
            mappedRegistry.unfreeze();
        }
    }

    private void onRegister(RegisterEvent event) {
        // Register custom codec extensions
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

        // ENTITY_TYPE is a Forge-tracked 'intrusive' registry.
        // It must be statically initialized exactly when Forge unlocks the NamespacedWrapper during this event bus broadcast.
        if (event.getRegistryKey().equals(BuiltInRegistries.ENTITY_TYPE.key())) {
            EnchantmentCoreEntities.initialize();
        }
    }

    private void onNewRegistry(NewRegistryEvent event) {
        for (net.minecraftforge.registries.RegistryBuilder<?> builder : ForgeRegistryHelper.PENDING_BUILDERS) {
            event.create(builder);
        }
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
        Path path = ModList.getModFileById(Constants.MOD_ID).getFile().findResource("resourcepacks", directory);
        if (Files.exists(path)) {
            boolean isRequired = inclusionType == PackInclusionType.REQUIRED;
            boolean isDefaultActive = inclusionType != PackInclusionType.OPTIONAL;

            PackSelectionConfig selectionConfig = new PackSelectionConfig(isRequired, Pack.Position.TOP, false);

            PackSource packSource = new PackSource() {
                @Override
                public @NotNull Component decorate(@NotNull Component component) {
                    return PackSource.BUILT_IN.decorate(component);
                }

                @Override
                public boolean shouldAddAutomatically() {
                    return isDefaultActive;
                }
            };

            Pack.ResourcesSupplier resourcesSupplier = new PathPackResources.PathResourcesSupplier(path);
            PackLocationInfo info = new PackLocationInfo(namespace + ":" + directory, Component.literal(name), packSource, Optional.empty());
            Pack pack = Pack.readMetaAndCreate(info, resourcesSupplier, PackType.SERVER_DATA, selectionConfig);

            if (pack != null) {
                event.addRepositorySource(consumer -> consumer.accept(pack));
            }
        }
    }
}