package johnsmith.enchantmentcore.entity;

import java.util.Map;
import java.util.function.Supplier;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.registry.EnchantmentCoreEntities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.RegisterEvent;

public final class ForgeEntities {
    public static void initialize(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.ENTITY_TYPE)) {
            for (Map.Entry<String, Supplier<EntityType<?>>> entry : EnchantmentCoreEntities.getEntities().entrySet()) {
                event.register(
                        Registries.ENTITY_TYPE,
                        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, entry.getKey()),
                        entry.getValue()
                );
            }
        }
    }

    private ForgeEntities() {}
}