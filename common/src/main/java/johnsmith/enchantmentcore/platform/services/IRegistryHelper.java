package johnsmith.enchantmentcore.platform.services;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.function.Supplier;

public interface IRegistryHelper {
    /**
     * Creates a custom registry and returns a lazy supplier.
     * On Forge/NeoForge, the supplier will not resolve until after NewRegistryEvent.
     */
    <T> Supplier<Registry<T>> createCustomRegistry(ResourceKey<Registry<T>> key);
}