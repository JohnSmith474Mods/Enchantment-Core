package johnsmith.enchantmentcore.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import johnsmith.enchantmentcore.platform.services.IRegistryHelper;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.RegistryBuilder;

public class ForgeRegistryHelper implements IRegistryHelper {

    // Cache the builders to be processed during the Forge lifecycle event
    public static final List<RegistryBuilder<?>> PENDING_BUILDERS = new ArrayList<>();

    @Override
    public <T> Supplier<Registry<T>> createCustomRegistry(ResourceKey<Registry<T>> key) {
        // .hasTags() is critical: it forces Forge to inject this registry into BuiltInRegistries.REGISTRY
        RegistryBuilder<T> builder = RegistryBuilder.<T>of(key.location()).hasTags();
        PENDING_BUILDERS.add(builder);

        // Return a lazy supplier that fetches the vanilla-mapped registry AFTER the event fires
        return () -> (Registry<T>) BuiltInRegistries.REGISTRY.get(key.location());
    }
}