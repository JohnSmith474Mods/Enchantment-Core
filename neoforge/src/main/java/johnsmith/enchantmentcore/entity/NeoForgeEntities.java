package johnsmith.enchantmentcore.entity;

import java.util.Map;
import java.util.function.Supplier;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.registry.EnchantmentCoreEntities;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgeEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Constants.MOD_ID);

    public static void initialize(IEventBus eventBus) {
        for (Map.Entry<String, Supplier<EntityType<?>>> entry : EnchantmentCoreEntities.getEntities().entrySet()) {
            ENTITIES.register(entry.getKey(), entry.getValue());
        }
        ENTITIES.register(eventBus);
    }

    private NeoForgeEntities() {}
}