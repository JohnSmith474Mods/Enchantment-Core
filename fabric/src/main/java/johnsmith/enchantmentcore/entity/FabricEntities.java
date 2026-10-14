package johnsmith.enchantmentcore.entity;

import java.util.Map;
import java.util.function.Supplier;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.registry.EnchantmentCoreEntities;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public final class FabricEntities {
    public static void initialize() {
        for (Map.Entry<String, Supplier<EntityType<?>>> entry : EnchantmentCoreEntities.getEntities().entrySet()) {
            Identifier location = Identifier.fromNamespaceAndPath(Constants.MOD_ID, entry.getKey());
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(location)) {
                Registry.register(BuiltInRegistries.ENTITY_TYPE, location, entry.getValue().get());
            }
        }
    }

    private FabricEntities() {}
}