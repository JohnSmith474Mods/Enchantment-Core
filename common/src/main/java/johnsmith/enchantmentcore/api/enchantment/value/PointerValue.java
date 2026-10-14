package johnsmith.enchantmentcore.api.enchantment.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.enchantment.LevelBasedValue;

import org.jetbrains.annotations.NotNull;

/**
 * A proxy {@link LevelBasedValue} used to tag and expose mathematical curves inside complex enchantment
 * JSON structures for dynamic tooltip interpolation.
 * <p>
 * During data loading, {@code EnchantmentCodecMixin} scans the enchantment's DFU tree, extracts each
 * {@link PointerValue} by its {@link #id()}, and binds its inner {@link #value()} into the
 * {@link johnsmith.enchantmentcore.api.registry.EnchantmentEffectComponents#DESCRIPTION_ARGS} component map
 * for $\mathcal{O}(1)$ runtime lookup by {@link johnsmith.enchantmentcore.api.tooltip.DynamicDescriptionParser}.
 *
 * @param id    The unique string key used to reference this value in translation strings (e.g., {@code <val:id>} or {@code <array:id>}).
 * @param value The underlying {@link LevelBasedValue} delegated to during calculation and tooltip rendering.
 */
public record PointerValue(
        String id,
        LevelBasedValue value
) implements LevelBasedValue {
    /** Registry path identifier for this level based value. */
    public static final String KEY = "pointer";

    /**
     * The codec responsible for serializing and deserializing {@link PointerValue} instances from JSON.
     */
    public static final MapCodec<PointerValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(PointerValue::id),
            LevelBasedValue.CODEC.fieldOf("value").forGetter(PointerValue::value)
    ).apply(instance, PointerValue::new));

    /**
     * Delegates calculation directly to the wrapped {@link #value()}.
     *
     * @param level The active level of the enchantment.
     * @return The calculated float value from the inner provider.
     */
    @Override
    public float calculate(int level) {
        return this.value.calculate(level);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link MapCodec} for this class.
     */
    @Override
    public @NotNull MapCodec<? extends LevelBasedValue> codec() {
        return CODEC;
    }
}