package johnsmith.enchantmentcore.registry;

import johnsmith.enchantmentcore.api.registry.EnchantmentCoreRegistries;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;

public final class FabricRegistries {
    public static void initialize() {
        FabricRegistryBuilder.create(EnchantmentCoreRegistries.DISTANCE_SCALING_FUNCTION_KEY).buildAndRegister();
        FabricRegistryBuilder.create(EnchantmentCoreRegistries.SPELL_FIELD_SHAPE_KEY).buildAndRegister();
        FabricRegistryBuilder.create(EnchantmentCoreRegistries.SPELL_FIELD_EFFECT_KEY).buildAndRegister();
    }

    private FabricRegistries() {}
}