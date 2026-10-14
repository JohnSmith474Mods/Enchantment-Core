package johnsmith.enchantmentcore.item.enchantment;

import com.mojang.serialization.MapCodec;

import java.util.Map;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.registry.EnchantmentValueRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public final class FabricLevelBasedValues {
    public static void initialize() {
        for (Map.Entry<String, MapCodec<? extends LevelBasedValue>> entry : EnchantmentValueRegistry.getValues().entrySet()) {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, entry.getKey());
            if (!BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE.containsKey(location)) {
                Registry.register(BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE, location, entry.getValue());
            }
        }
    }

    private FabricLevelBasedValues() {}
}