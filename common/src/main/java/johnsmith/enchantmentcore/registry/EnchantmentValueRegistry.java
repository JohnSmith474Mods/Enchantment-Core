package johnsmith.enchantmentcore.registry;

import com.mojang.serialization.MapCodec;

import johnsmith.enchantmentcore.*;
import johnsmith.enchantmentcore.enchantment.value.DiminishingReturnsValue;
import johnsmith.enchantmentcore.enchantment.value.NegateValue;
import johnsmith.enchantmentcore.enchantment.value.PolynomialValue;
import johnsmith.enchantmentcore.enchantment.value.ProbabilisticValue;
import johnsmith.enchantmentcore.enchantment.value.configurable.*;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Registers custom mathematical formulas into the vanilla LevelBasedValue ecosystem.
 * Allows enchantments to scale their variables non-linearly or retrieve externally defined values.
 */
public class EnchantmentValueRegistry {

    /**
     * Injects all custom mathematical codecs into the built-in LevelBasedValue registry.
     * Must be called during the mod initialization sequence.
     */
    public static void initialize() {
        register("configurable_constant", ConfigurableConstantValue.CODEC);
        register("configurable_clamped", ConfigurableClampedValue.CODEC);
        register("configurable_diminishing_returns", ConfigurableDiminishingReturnsValue.CODEC);
        register("configurable_levels_squared", ConfigurableLevelsSquaredValue.CODEC);
        register("configurable_linear", ConfigurableLinearValue.CODEC);
        register("configurable_polynomial", ConfigurablePolynomialValue.CODEC);
        register("diminishing_returns", DiminishingReturnsValue.CODEC);
        register("polynomial", PolynomialValue.CODEC);
        register("config_aware", ConfigAwareValue.CODEC);
        register("probabilistic", ProbabilisticValue.CODEC);
        register("negative", NegateValue.CODEC);
    }

    /**
     * Helper method to map a codec directly into the LevelBasedValue registry.
     *
     * @param name  The identifier segment of the resource location.
     * @param codec The specific map codec implementation for the formula structure.
     */
    private static void register(String name, MapCodec<? extends LevelBasedValue> codec) {
        Registry.register(BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name), codec);
    }
}