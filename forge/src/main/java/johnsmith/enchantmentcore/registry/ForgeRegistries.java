package johnsmith.enchantmentcore.registry;

import com.mojang.serialization.MapCodec;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpellFieldShape;
import johnsmith.enchantmentcore.api.registry.EnchantmentCoreRegistries;

import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

public final class ForgeRegistries {
    public static void initialize(NewRegistryEvent event) {
        // .hasTags() forces Forge to inject custom static registries into BuiltInRegistries.REGISTRY
        event.create(RegistryBuilder.<MapCodec<? extends DistanceScalingFunction>>of(
                EnchantmentCoreRegistries.DISTANCE_SCALING_FUNCTION_KEY.identifier()
        ).hasTags());
        event.create(RegistryBuilder.<MapCodec<? extends SpellFieldShape>>of(
                EnchantmentCoreRegistries.SPELL_FIELD_SHAPE_KEY.identifier()
        ).hasTags());
        event.create(RegistryBuilder.<MapCodec<? extends SpellFieldEffect>>of(
                EnchantmentCoreRegistries.SPELL_FIELD_EFFECT_KEY.identifier()
        ).hasTags());
    }

    private ForgeRegistries() {}
}