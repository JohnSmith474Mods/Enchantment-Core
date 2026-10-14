package johnsmith.enchantmentcore.registry;

import johnsmith.enchantmentcore.api.registry.EnchantmentCoreRegistries;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

public final class NeoForgeRegistries {
    public static void initialize(NewRegistryEvent event) {
        event.create(new RegistryBuilder<>(EnchantmentCoreRegistries.DISTANCE_SCALING_FUNCTION_KEY));
        event.create(new RegistryBuilder<>(EnchantmentCoreRegistries.SPELL_FIELD_SHAPE_KEY));
        event.create(new RegistryBuilder<>(EnchantmentCoreRegistries.SPELL_FIELD_EFFECT_KEY));
    }

    private NeoForgeRegistries() {}
}