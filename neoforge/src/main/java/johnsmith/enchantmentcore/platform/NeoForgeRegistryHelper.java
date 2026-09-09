package johnsmith.enchantmentcore.platform;

import com.mojang.serialization.Lifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.platform.services.IRegistryHelper;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NeoForgeRegistryHelper implements IRegistryHelper {

    public static final List<Registry<?>> PENDING_REGISTRIES = new ArrayList<>();

    // Cache active DeferredRegister instances mapped by their target ResourceKey
    private static final Map<ResourceKey<?>, DeferredRegister<?>> DEFERRED_REGISTERS = new HashMap<>();

    @Override
    public <T> Supplier<Registry<T>> createCustomRegistry(ResourceKey<Registry<T>> key) {
        // Instantiate the registry immediately
        Registry<T> registry = new MappedRegistry<>(key, Lifecycle.stable());

        // Queue it for the NeoForge NewRegistryEvent
        PENDING_REGISTRIES.add(registry);

        // Return a lazy supplier wrapping the local instance
        return () -> registry;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R extends T> Supplier<R> register(Registry<T> registry, String name, Supplier<R> supplier) {
        DeferredRegister<T> deferredRegister = (DeferredRegister<T>) DEFERRED_REGISTERS.computeIfAbsent(
                registry.key(),
                k -> DeferredRegister.create(registry.key(), Constants.MOD_ID)
        );
        return deferredRegister.register(name, supplier);
    }

    /**
     * Registers all active DeferredRegisters to the primary mod event bus.
     */
    public static void registerAll(IEventBus modEventBus) {
        DEFERRED_REGISTERS.values().forEach(deferredRegister -> deferredRegister.register(modEventBus));
    }
}