package johnsmith.enchantmentcore.item.enchantment;

import com.mojang.serialization.MapCodec;

import java.util.Map;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.registry.EnchantmentValueRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraftforge.registries.RegisterEvent;

public final class ForgeLevelBasedValues {

    public static void initialize(RegisterEvent event) {
        if (event.getRegistryKey().equals(BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE.key())) {
            for (Map.Entry<String, MapCodec<? extends LevelBasedValue>> entry : EnchantmentValueRegistry.getValues().entrySet()) {
                Identifier location = Identifier.fromNamespaceAndPath(Constants.MOD_ID, entry.getKey());
                if (!BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE.containsKey(location)) {
                    event.register(
                            BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE.key(),
                            location,
                            entry::getValue
                    );
                }
            }
        }
    }

    private ForgeLevelBasedValues() {}
}