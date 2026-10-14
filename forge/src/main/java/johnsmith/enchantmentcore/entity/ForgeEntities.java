package johnsmith.enchantmentcore.entity;

import java.util.Map;
import java.util.function.Supplier;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.registry.EnchantmentCoreEntities;

import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgeEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Constants.MOD_ID);

    public static void initialize(IEventBus eventBus) {
        for (Map.Entry<String, Supplier<EntityType<?>>> entry : EnchantmentCoreEntities.getEntities().entrySet()) {
            ENTITIES.register(entry.getKey(), entry.getValue());
        }
        ENTITIES.register(eventBus);
    }

    private ForgeEntities() {}
}