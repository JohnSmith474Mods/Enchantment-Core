package johnsmith.enchantmentcore.enchantment.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import org.jetbrains.annotations.NotNull;

/**
 * A proxy {@link LevelBasedValue} used to mark mathematical curves inside complex JSON structures.
 * <p>
 * During data loading, the {@code EnchantmentCodecMixin} actively scans the DFU tree, extracts these
 * pointers by their {@code id}, and automatically injects their child {@code value} directly into
 * the {@code enchantment_core:description_args} component map for O(1) runtime lookup.
 */
public record PointerValue(
        String id,
        LevelBasedValue value
) implements LevelBasedValue {

    public static final MapCodec<PointerValue> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(PointerValue::id),
            LevelBasedValue.CODEC.fieldOf("value").forGetter(PointerValue::value)
    ).apply(instance, PointerValue::new));

    @Override
    public float calculate(int level) {
        return this.value.calculate(level);
    }

    @Override
    public @NotNull MapCodec<? extends LevelBasedValue> codec() {
        return CODEC;
    }
}