package johnsmith.enchantmentcore.item.enchantment;

import com.mojang.serialization.MapCodec;

import java.util.Map;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.registry.EnchantmentEffectComponents;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraftforge.registries.RegisterEvent;

public final class ForgeEnchantmentEffectComponents {
    public static void initialize(RegisterEvent event) {
        if (event.getRegistryKey().equals(BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE.key())) {
            for (Map.Entry<String, MapCodec<? extends EnchantmentEntityEffect>> entry : EnchantmentEffectComponents.getEntityEffectTypes().entrySet()) {
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, entry.getKey());
                if (!BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE.containsKey(location)) {
                    event.register(
                            BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE.key(),
                            location,
                            entry::getValue
                    );
                }
            }
        }

        if (event.getRegistryKey().equals(BuiltInRegistries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE.key())) {
            for (Map.Entry<String, MapCodec<? extends EnchantmentLocationBasedEffect>> entry : EnchantmentEffectComponents.getLocationBasedEffectTypes().entrySet()) {
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, entry.getKey());
                if (!BuiltInRegistries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE.containsKey(location)) {
                    event.register(
                            BuiltInRegistries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE.key(),
                            location,
                            entry::getValue
                    );
                }
            }
        }

        if (event.getRegistryKey().equals(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.key())) {
            for (Map.Entry<String, DataComponentType<?>> entry : EnchantmentEffectComponents.getEffectComponentTypes().entrySet()) {
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, entry.getKey());
                if (!BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.containsKey(location)) {
                    event.register(
                            BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.key(),
                            location,
                            entry::getValue
                    );
                }
            }
        }

        if (event.getRegistryKey().equals(BuiltInRegistries.DATA_COMPONENT_TYPE.key())) {
            for (Map.Entry<String, DataComponentType<?>> entry : EnchantmentEffectComponents.getItemDataComponentTypes().entrySet()) {
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, entry.getKey());
                if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(location)) {
                    event.register(
                            BuiltInRegistries.DATA_COMPONENT_TYPE.key(),
                            location,
                            entry::getValue
                    );
                }
            }
        }
    }

    private ForgeEnchantmentEffectComponents() {}
}