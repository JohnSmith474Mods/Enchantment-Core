package johnsmith.enchantmentcore.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.platform.services.IRegistryHelper;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryBuilder;

public class ForgeRegistryHelper implements IRegistryHelper {

    // Cache the builders to be processed during the Forge lifecycle event
    public static final List<RegistryBuilder<?>> PENDING_BUILDERS = new ArrayList<>();

    // Cache active DeferredRegister instances mapped by their target ResourceKey
    private static final Map<ResourceKey<?>, DeferredRegister<?>> DEFERRED_REGISTERS = new HashMap<>();

    @Override
    public <T> Supplier<Registry<T>> createCustomRegistry(ResourceKey<Registry<T>> key) {
        // .hasTags() is critical: it forces Forge to inject this registry into BuiltInRegistries.REGISTRY
        RegistryBuilder<T> builder = RegistryBuilder.<T>of(key.location()).hasTags();
        PENDING_BUILDERS.add(builder);

        // Return a lazy supplier that fetches the vanilla-mapped registry AFTER the event fires
        return () -> (Registry<T>) BuiltInRegistries.REGISTRY.get(key.location()).get().value();
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