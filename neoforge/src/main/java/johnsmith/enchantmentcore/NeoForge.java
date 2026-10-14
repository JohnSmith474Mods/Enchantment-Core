package johnsmith.enchantmentcore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import johnsmith.enchantmentcore.api.config.PackInclusionType;
import johnsmith.enchantmentcore.command.ExportCommand;
import johnsmith.enchantmentcore.command.GenerateDescriptionKeysCommand;
import johnsmith.enchantmentcore.command.ImportCommand;
import johnsmith.enchantmentcore.command.SpellFieldCommand;
import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.entity.NeoForgeEntities;
import johnsmith.enchantmentcore.item.enchantment.NeoForgeEnchantmentEffectComponents;
import johnsmith.enchantmentcore.item.enchantment.NeoForgeLevelBasedValues;
import johnsmith.enchantmentcore.item.enchantment.spellfield.NeoForgeSpellFieldRegistries;
import johnsmith.enchantmentcore.registry.NeoForgeRegistries;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;


import org.jetbrains.annotations.NotNull;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

@Mod(Constants.MOD_ID)
public class NeoForge {

    public NeoForge(IEventBus eventBus, ModContainer container) {
        // Resolve Config Path
        Config.MANAGER.init(FMLPaths.CONFIGDIR.get());

        // Initialize common classes
        Common.initialize(ModList.get()::isLoaded);

        // Register Library Components
        NeoForgeEntities.initialize(eventBus);
        eventBus.addListener(NeoForgeRegistries::initialize);
        eventBus.addListener(NeoForgeSpellFieldRegistries::initialize);
        eventBus.addListener(NeoForgeLevelBasedValues::initialize);
        eventBus.addListener(NeoForgeEnchantmentEffectComponents::initialize);

        // Register Optional Data
        eventBus.addListener(this::onAddPackFinders);

        // Register Commands
        EVENT_BUS.addListener(this::onRegisterCommands);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            container.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (client, parent) -> Config.MANAGER.createScreen(parent)
            );
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ImportCommand.register(event.getDispatcher());
        GenerateDescriptionKeysCommand.register(event.getDispatcher());
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
        Path path = null;

        for (Path root : ModList.get().getModFileById(Constants.MOD_ID).getFile().getContents().getContentRoots()) {
            Path resolved = root.resolve("resourcepacks").resolve(directory);
            if (Files.exists(resolved)) {
                path = resolved;
                break;
            }
        }

        if (path != null) {
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