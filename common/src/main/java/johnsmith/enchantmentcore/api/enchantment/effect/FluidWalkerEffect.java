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
 * Standard record implementation of {@link FluidWalkerDefinition} enabling entities to stand and walk
 * atop specified fluid blocks without modifying world block states.
 *
 * @param allowedFluids  The {@link HolderSet} of fluids treated as solid surfaces when approached from above.
 * @param speedRetention The {@link LevelBasedValue} movement speed multiplier while walking on matching fluids.
 */
public record FluidWalkerEffect(
        HolderSet<Fluid> allowedFluids,
        LevelBasedValue speedRetention
) implements FluidWalkerDefinition {
    /** JSON field key for the set of walkable fluids. */
    public static final String ALLOWED_FLUIDS = "allowed_fluids";

    /** JSON field key for the surface movement speed multiplier. */
    public static final String SPEED_RETENTION = "speed_retention";

    /** Registry path identifier for this effect component. */
    public static final String KEY = "fluid_walker";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(SPEED_RETENTION);

    /**
     * Codec for serializing and deserializing {@link FluidWalkerEffect} instances from JSON.
     */
    public static final Codec<FluidWalkerEffect> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RegistryCodecs.homogeneousList(Registries.FLUID).fieldOf(ALLOWED_FLUIDS).forGetter(FluidWalkerEffect::allowedFluids),
                    LevelBasedValue.CODEC.optionalFieldOf(SPEED_RETENTION, LevelBasedValue.constant(1.0F)).forGetter(FluidWalkerEffect::speedRetention)
            ).apply(instance, FluidWalkerEffect::new)
    );
}