package johnsmith.enchantmentcore.platform;

import johnsmith.enchantmentcore.platform.services.IRegistryHelper;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import com.mojang.serialization.Lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class NeoForgeRegistryHelper implements IRegistryHelper {
    public static final List<Registry<?>> PENDING_REGISTRIES = new ArrayList<>();

    @Override
    public <T> Supplier<Registry<T>> createCustomRegistry(ResourceKey<Registry<T>> key) {
        // Instantiate the registry immediately
        Registry<T> registry = new MappedRegistry<>(key, Lifecycle.stable());

        // Queue it for the NeoForge NewRegistryEvent
        PENDING_REGISTRIES.add(registry);

        // Return a lazy supplier wrapping the local instance
        return () -> registry;
    }
}