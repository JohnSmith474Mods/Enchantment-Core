package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.material.Fluid;

/**
 * A data-driven record defining an enchantment component effect that modifies fog/visibility
 * when the player is submerged in certain fluids.
 * <p>
 * This is primarily used by head armor enchantments to clear underwater or lava visibility
 * beyond the vanilla range by adjusting the shader fog start and end distances in {@code FogRendererMixin}.
 *
 * @param fluids           A set of fluid tags or IDs that this effect should apply to (e.g., {@code #minecraft:water}).
 * @param fogStart         A {@link LevelBasedValue} to calculate the start distance of the fog.
 * @param fogEndMultiplier A {@link LevelBasedValue} to calculate a multiplier for the view distance
 *                         which determines the final end distance of the fog.
 */
public record FluidFogDensityEffect(
        HolderSet<Fluid> fluids,
        LevelBasedValue fogStart,
        LevelBasedValue fogEndMultiplier
) {
    /** JSON field key for the set of fluids affected by this fog modifier. */
    public static final String FLUIDS = "fluids";

    /** JSON field key for the shader fog start distance. */
    public static final String FOG_START = "fog_start";

    /** JSON field key for the view-distance multiplier governing shader fog end distance. */
    public static final String FOG_END_MULTIPLIER = "fog_end_multiplier";

    /** Registry path identifier for this effect component. */
    public static final String KEY = "fluid_fog_density";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
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