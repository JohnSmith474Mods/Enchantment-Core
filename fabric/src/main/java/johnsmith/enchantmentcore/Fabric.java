package johnsmith.enchantmentcore;

import johnsmith.enchantmentcore.api.config.PackInclusionType;
import johnsmith.enchantmentcore.command.ExportCommand;
import johnsmith.enchantmentcore.command.GenerateDescriptionKeysCommand;
import johnsmith.enchantmentcore.command.ImportCommand;
import johnsmith.enchantmentcore.command.SpellFieldCommand;
import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.config.Orphanage;
import johnsmith.enchantmentcore.entity.FabricEntities;
import johnsmith.enchantmentcore.item.enchantment.FabricEnchantmentEffectComponents;
import johnsmith.enchantmentcore.item.enchantment.FabricLevelBasedValues;
import johnsmith.enchantmentcore.item.enchantment.spellfield.FabricSpellFieldRegistries;
import johnsmith.enchantmentcore.registry.FabricRegistries;
import johnsmith.enchantmentcore.util.SpellFieldTaskScheduler;
import johnsmith.enchantmentcore.util.TransientBlockTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Fabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // Resolve Config Path
        Config.MANAGER.init(FabricLoader.getInstance().getConfigDir());

        // Initialize common classes
        Common.initialize(FabricLoader.getInstance()::isModLoaded);

        // Initialize Custom Registries & Library Components
        FabricRegistries.initialize();
        FabricSpellFieldRegistries.initialize();
        FabricLevelBasedValues.initialize();
        FabricEnchantmentEffectComponents.initialize();
        FabricEntities.initialize();

        // Register Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> {
            ExportCommand.register(dispatcher);
            GenerateDescriptionKeysCommand.register(dispatcher);
            ImportCommand.register(dispatcher);
            SpellFieldCommand.register(dispatcher);
        });

        // Register Server World Tick Handlers
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            SpellFieldTaskScheduler.tick(level);
            TransientBlockTracker.get(level).tick(level);
        });

        // Register Orphan Notification
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Orphanage.notifyPlayerIfPrivileged(handler.player);
        });

        // Register Optional Data
        this.registerBuiltInPack(Constants.MOD_ID, "test_enchantments", "Enchantment Core Test Enchantments", PackInclusionType.OPTIONAL);
        this.registerBuiltInPack("minecraft", "default_enchantments", "Configurable Default Enchantments", Config.CONFIGURABLE_DEFAULT_ENCHANTMENTS.get());
    }

    private void registerBuiltInPack(String namespace, String directory, String name, PackInclusionType inclusionType) {
        ResourcePackActivationType activationType = switch (inclusionType) {
            case REQUIRED -> ResourcePackActivationType.ALWAYS_ENABLED;
            case ACTIVE -> ResourcePackActivationType.DEFAULT_ENABLED;
            case OPTIONAL -> ResourcePackActivationType.NORMAL;
        };

        FabricLoader.getInstance().getModContainer(Constants.MOD_ID).ifPresent(container -> {
            ResourceManagerHelper.registerBuiltinResourcePack(
                    ResourceLocation.fromNamespaceAndPath(namespace, directory),
                    container,
                    Component.literal(name),
                    activationType
            );
        });
    }
}