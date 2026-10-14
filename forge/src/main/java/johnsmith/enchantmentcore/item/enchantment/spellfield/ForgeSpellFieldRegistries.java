package johnsmith.enchantmentcore.item.enchantment.spellfield;

import com.mojang.serialization.MapCodec;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpellFieldShape;
import johnsmith.enchantmentcore.api.registry.EnchantmentCoreRegistries;
import johnsmith.enchantmentcore.api.registry.SpellFieldRegistrar;

import net.minecraft.core.Registry;

import net.minecraftforge.registries.RegisterEvent;

public final class ForgeSpellFieldRegistries {
    public static void initialize(RegisterEvent event) {
        if (event.getRegistryKey().equals(EnchantmentCoreRegistries.DISTANCE_SCALING_FUNCTION_KEY)) {
            Registry<MapCodec<? extends DistanceScalingFunction>> registry = EnchantmentCoreRegistries.DISTANCE_SCALING_FUNCTIONS.get();
            SpellFieldRegistrar.registerFunctions((id, codec) -> {
                if (registry == null || !registry.containsKey(id)) {
                    event.register(EnchantmentCoreRegistries.DISTANCE_SCALING_FUNCTION_KEY, id, () -> codec);
                }
            });
        }

        if (event.getRegistryKey().equals(EnchantmentCoreRegistries.SPELL_FIELD_SHAPE_KEY)) {
            Registry<MapCodec<? extends SpellFieldShape>> registry = EnchantmentCoreRegistries.SPELL_FIELD_SHAPES.get();
            SpellFieldRegistrar.registerShapes((id, codec) -> {
                if (registry == null || !registry.containsKey(id)) {
                    event.register(EnchantmentCoreRegistries.SPELL_FIELD_SHAPE_KEY, id, () -> codec);
                }
            });
        }

        if (event.getRegistryKey().equals(EnchantmentCoreRegistries.SPELL_FIELD_EFFECT_KEY)) {
            Registry<MapCodec<? extends SpellFieldEffect>> registry = EnchantmentCoreRegistries.SPELL_FIELD_EFFECTS.get();
            SpellFieldRegistrar.registerEffects((id, codec) -> {
                if (registry == null || !registry.containsKey(id)) {
                    event.register(EnchantmentCoreRegistries.SPELL_FIELD_EFFECT_KEY, id, () -> codec);
                }
            });
        }
    }

    private ForgeSpellFieldRegistries() {}
}