package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.material.Fluid;

import java.util.Set;

/**
 * A data-driven record defining an enchantment component effect that modifies fog/visibility
 * when the player is submerged in certain fluids.
 * <p>
 * This is primarily used by the Aqua Affinity enchantment to clear underwater visibility
 * beyond the vanilla range by adjusting the shader fog start and end distances.
 *
 * @param fluids              A set of fluid tags or IDs that this effect should apply to (e.g., {@code #minecraft:water}).
 * @param fogStart            A {@link LevelBasedValue} to calculate the start distance of the fog.
 * @param fogEndMultiplier    A {@link LevelBasedValue} to calculate a multiplier for the view distance
 * which determines the final end distance of the fog.
 */
public record FluidFogDensityEffect(
        HolderSet<Fluid> fluids,
        LevelBasedValue fogStart,
        LevelBasedValue fogEndMultiplier
) {
    public static final String FLUIDS = "fluids";
    public static final String FOG_START = "fog_start";
    public static final String FOG_END_MULTIPLIER = "fog_end_multiplier";

    public static final String KEY = "fluid_fog_density";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(FOG_START, FOG_END_MULTIPLIER);

    /**
     * The codec responsible for serializing and deserializing instances of this record from data files (e.g., JSON).
     * <p>
     * It uses {@link RegistryCodecs#homogeneousList} to properly handle the {@link HolderSet} of fluids.
     */
    public static final Codec<FluidFogDensityEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.FLUID).fieldOf(FLUIDS).forGetter(FluidFogDensityEffect::fluids),
            LevelBasedValue.CODEC.fieldOf(FOG_START).forGetter(FluidFogDensityEffect::fogStart),
            LevelBasedValue.CODEC.fieldOf(FOG_END_MULTIPLIER).forGetter(FluidFogDensityEffect::fogEndMultiplier)
    ).apply(instance, FluidFogDensityEffect::new));
}