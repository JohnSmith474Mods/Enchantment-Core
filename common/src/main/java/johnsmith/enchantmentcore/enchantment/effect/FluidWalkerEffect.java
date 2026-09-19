package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.effect.FluidWalkerDefinition;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.material.Fluid;

public record FluidWalkerEffect(
        HolderSet<Fluid> allowedFluids,
        LevelBasedValue speedRetention
) implements FluidWalkerDefinition {

    public static final String ALLOWED_FLUIDS = "allowed_fluids";
    public static final String SPEED_RETENTION = "speed_retention";

    public static final String KEY = "fluid_walker";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(SPEED_RETENTION);

    public static final Codec<FluidWalkerEffect> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RegistryCodecs.homogeneousList(Registries.FLUID).fieldOf(ALLOWED_FLUIDS).forGetter(FluidWalkerEffect::allowedFluids),
                    LevelBasedValue.CODEC.optionalFieldOf(SPEED_RETENTION, LevelBasedValue.constant(1.0F)).forGetter(FluidWalkerEffect::speedRetention)
            ).apply(instance, FluidWalkerEffect::new)
    );
}