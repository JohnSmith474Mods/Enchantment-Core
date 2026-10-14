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
import johnsmith.enchantmentcore.entity.ForgeEntities;
import johnsmith.enchantmentcore.item.enchantment.ForgeEnchantmentEffectComponents;
import johnsmith.enchantmentcore.item.enchantment.ForgeLevelBasedValues;
import johnsmith.enchantmentcore.item.enchantment.spellfield.ForgeSpellFieldRegistries;
import johnsmith.enchantmentcore.registry.ForgeRegistries;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
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

        // Resolve Config Path
        Config.MANAGER.init(FMLPaths.CONFIGDIR.get());

        // Initialize common classes
        Common.initialize(ModList.get()::isLoaded);

        // Register Library Components
        NewRegistryEvent.getBus(modBusGroup).addListener(ForgeRegistries::initialize);
        RegisterEvent.getBus(modBusGroup).addListener(ForgeEntities::initialize);
        RegisterEvent.getBus(modBusGroup).addListener(ForgeSpellFieldRegistries::initialize);
        RegisterEvent.getBus(modBusGroup).addListener(ForgeLevelBasedValues::initialize);
        RegisterEvent.getBus(modBusGroup).addListener(ForgeEnchantmentEffectComponents::initialize);

        // Register Optional Data
        AddPackFindersEvent.getBus(modBusGroup).addListener(this::onAddPackFinders);

        // Register Commands
        RegisterCommandsEvent.BUS.addListener(this::onRegisterCommands);

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
        Path path = ModList.get().getModFileById(Constants.MOD_ID).getFile().findResource("resourcepacks", directory);
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