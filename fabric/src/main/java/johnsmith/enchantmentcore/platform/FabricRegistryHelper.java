package johnsmith.enchantmentcore.platform;

import java.util.function.Supplier;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.platform.services.IRegistryHelper;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class FabricRegistryHelper implements IRegistryHelper {
    @Override
    public <T> Supplier<Registry<T>> createCustomRegistry(ResourceKey<Registry<T>> key) {
        Registry<T> registry = FabricRegistryBuilder.createSimple(key).buildAndRegister();
        return () -> registry;
    }

    @Override
    public <T, R extends T> Supplier<R> register(Registry<T> registry, String name, Supplier<R> supplier) {
        R registered = Registry.register(registry, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name), supplier.get());
        return () -> registered;
    }
}