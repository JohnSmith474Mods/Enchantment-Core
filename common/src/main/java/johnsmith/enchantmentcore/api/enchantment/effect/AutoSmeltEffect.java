package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Data-driven enchantment effect component that automatically smelts block drops using active
 * {@link net.minecraft.world.item.crafting.RecipeType#SMELTING} recipes when breaking blocks.
 * <p>
 * Evaluated during block drop generation in {@code EnchantmentEventsMixin}. Supports optional
 * per-smelted-item tool durability penalties and fractional smelting experience accumulation via
 * {@link johnsmith.enchantmentcore.api.registry.EnchantmentEffectComponents#STORED_SMELTING_XP}.
 *
 * @param additionalToolUsage The {@link LevelBasedValue} specifying extra durability damage incurred per smelted item.
 * @param dropXp              Whether smelting recipe experience should be awarded when items are auto-smelted.
 */
public record AutoSmeltEffect(
        LevelBasedValue additionalToolUsage,
        boolean dropXp
) {
    /** JSON field key for the extra tool durability cost per smelted drop. */
    public static final String ADDITIONAL_TOOL_USAGE = "additional_tool_usage";

    /** JSON field key for enabling or disabling smelting experience drops. */
    public static final String DROP_XP = "drop_xp";

    /** Registry path identifier for this effect component. */
    public static final String KEY = "auto_smelt";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(ADDITIONAL_TOOL_USAGE);

    /**
     * Codec for serializing and deserializing {@link AutoSmeltEffect} instances from JSON.
     */
    public static final Codec<AutoSmeltEffect> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    LevelBasedValue.CODEC.optionalFieldOf(ADDITIONAL_TOOL_USAGE, LevelBasedValue.constant(0)).forGetter(AutoSmeltEffect::additionalToolUsage),
                    Codec.BOOL.optionalFieldOf(DROP_XP, true).forGetter(AutoSmeltEffect::dropXp)
            ).apply(instance, AutoSmeltEffect::new)
    );
}