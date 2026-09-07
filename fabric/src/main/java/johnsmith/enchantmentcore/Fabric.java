package johnsmith.enchantmentcore;

import johnsmith.enchantmentcore.api.config.PackInclusionType;
import johnsmith.enchantmentcore.command.ImportCommand;
import johnsmith.enchantmentcore.command.ExportCommand;
import johnsmith.enchantmentcore.command.SpellFieldCommand;
import johnsmith.enchantmentcore.config.Orphanage;
import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.registry.EnchantmentValueRegistry;
import johnsmith.enchantmentcore.registry.EnchantmentCoreEntities;
import johnsmith.enchantmentcore.registry.EnchantmentCoreRegistries;
import johnsmith.enchantmentcore.registry.SpellFieldRegistrar;
import johnsmith.enchantmentcore.util.SpellFieldTaskScheduler;
import johnsmith.enchantmentcore.util.TransientBlockTracker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Fabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Initialize common classes
        Common.initialize();

        // Initialize Registries
        SpellFieldRegistrar.registerFunctions((id, codec) ->
                Registry.register(EnchantmentCoreRegistries.DISTANCE_SCALING_FUNCTIONS.get(), id, codec));

        SpellFieldRegistrar.registerShapes((id, codec) ->
                Registry.register(EnchantmentCoreRegistries.SPELL_FIELD_SHAPES.get(), id, codec));

        SpellFieldRegistrar.registerEffects((id, codec) ->
                Registry.register(EnchantmentCoreRegistries.SPELL_FIELD_EFFECTS.get(), id, codec));

        // Resolve Config Path
        Config.MANAGER.init(FabricLoader.getInstance().getConfigDir());

        // Register Library Components
        EnchantmentValueRegistry.initialize();
        EnchantmentEffectComponentRegistry.initialize();
        EnchantmentCoreEntities.initialize();

        // Register Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> {
            ExportCommand.register(dispatcher);
            ImportCommand.register(dispatcher);
            SpellFieldCommand.register(dispatcher);
        });

        // Register SpellFieldTaskScheduler
        ServerTickEvents.END_WORLD_TICK.register(SpellFieldTaskScheduler::tick);

        // Register TransientBlockTracker
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
