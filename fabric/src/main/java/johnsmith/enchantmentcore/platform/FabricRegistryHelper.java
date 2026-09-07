package johnsmith.enchantmentcore.platform;

import java.util.function.Supplier;

import johnsmith.enchantmentcore.platform.services.IRegistryHelper;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class FabricRegistryHelper implements IRegistryHelper {
    @Override
    public <T> Supplier<Registry<T>> createCustomRegistry(ResourceKey<Registry<T>> key) {
        Registry<T> registry = FabricRegistryBuilder.createSimple(key).buildAndRegister();
        return () -> registry;
    }
}