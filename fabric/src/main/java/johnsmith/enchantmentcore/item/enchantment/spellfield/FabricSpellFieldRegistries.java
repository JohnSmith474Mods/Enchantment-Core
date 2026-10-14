package johnsmith.enchantmentcore.item.enchantment.spellfield;

import com.mojang.serialization.MapCodec;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpellFieldShape;
import johnsmith.enchantmentcore.api.registry.EnchantmentCoreRegistries;
import johnsmith.enchantmentcore.api.registry.SpellFieldRegistrar;

import net.minecraft.core.Registry;

public final class FabricSpellFieldRegistries {
    public static void initialize() {
        Registry<MapCodec<? extends DistanceScalingFunction>> functionRegistry = EnchantmentCoreRegistries.DISTANCE_SCALING_FUNCTIONS.get();
        SpellFieldRegistrar.registerFunctions((id, codec) -> {
            if (!functionRegistry.containsKey(id)) {
                Registry.register(functionRegistry, id, codec);
            }
        });

        Registry<MapCodec<? extends SpellFieldShape>> shapeRegistry = EnchantmentCoreRegistries.SPELL_FIELD_SHAPES.get();
        SpellFieldRegistrar.registerShapes((id, codec) -> {
            if (!shapeRegistry.containsKey(id)) {
                Registry.register(shapeRegistry, id, codec);
            }
        });

        Registry<MapCodec<? extends SpellFieldEffect>> effectRegistry = EnchantmentCoreRegistries.SPELL_FIELD_EFFECTS.get();
        SpellFieldRegistrar.registerEffects((id, codec) -> {
            if (!effectRegistry.containsKey(id)) {
                Registry.register(effectRegistry, id, codec);
            }
        });
    }

    private FabricSpellFieldRegistries() {}
}