package johnsmith.enchantmentcore.item.enchantment;

import com.mojang.serialization.MapCodec;

import java.util.Map;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.registry.EnchantmentEffectComponents;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;

public final class FabricEnchantmentEffectComponents {
    public static void initialize() {
        for (Map.Entry<String, MapCodec<? extends EnchantmentEntityEffect>> entry : EnchantmentEffectComponents.getEntityEffectTypes().entrySet()) {
            Identifier location = Identifier.fromNamespaceAndPath(Constants.MOD_ID, entry.getKey());
            if (!BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE.containsKey(location)) {
                Registry.register(BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE, location, entry.getValue());
            }
        }

        for (Map.Entry<String, MapCodec<? extends EnchantmentLocationBasedEffect>> entry : EnchantmentEffectComponents.getLocationBasedEffectTypes().entrySet()) {
            Identifier location = Identifier.fromNamespaceAndPath(Constants.MOD_ID, entry.getKey());
            if (!BuiltInRegistries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE.containsKey(location)) {
                Registry.register(BuiltInRegistries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE, location, entry.getValue());
            }
        }

        for (Map.Entry<String, DataComponentType<?>> entry : EnchantmentEffectComponents.getEffectComponentTypes().entrySet()) {
            Identifier location = Identifier.fromNamespaceAndPath(Constants.MOD_ID, entry.getKey());
            if (!BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.containsKey(location)) {
                Registry.register(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, location, entry.getValue());
            }
        }

        for (Map.Entry<String, DataComponentType<?>> entry : EnchantmentEffectComponents.getItemDataComponentTypes().entrySet()) {
            Identifier location = Identifier.fromNamespaceAndPath(Constants.MOD_ID, entry.getKey());
            if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(location)) {
                Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, location, entry.getValue());
            }
        }
    }

    private FabricEnchantmentEffectComponents() {}
}