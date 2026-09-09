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

    /**
     * Queues an object for registration against a targeted registry.
     *
     * @param registry The target registry.
     * @param name     The resource path identifier.
     * @param supplier A supplier yielding the object to register.
     * @param <T>      The base type of the registry.
     * @param <R>      The specific type of the registered object.
     * @return A supplier resolving the registered object.
     */
    <T, R extends T> Supplier<R> register(Registry<T> registry, String name, Supplier<R> supplier);
}