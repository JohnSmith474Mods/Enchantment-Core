package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.world.item.enchantment.LevelBasedValue;

import java.util.Set;

/**
 * A data-driven record defining the parameters for an enchantment component that affects bow charge time.
 * <p>
 * This effect is used to calculate the reduction in charge duration (in seconds) applied to a ranged weapon,
 * such as through the Quick Charge enchantment, by modifying the effective maximum charge time.
 *
 * @param amount A {@link LevelBasedValue} that calculates the total amount of time reduction (in seconds)
 * at a specific enchantment level. This value is typically negative.
 */
public record BowChargeTimeEffect(
        LevelBasedValue amount
) {
    public static final String AMOUNT = "amount";

    public static final String KEY = "bow_charge_time";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(AMOUNT);

    /**
     * The codec responsible for serializing and deserializing instances of this record from data files (e.g., JSON).
     * It maps the {@code "amount"} field to a {@link LevelBasedValue}.
     */
    public static final Codec<BowChargeTimeEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(AMOUNT).forGetter(BowChargeTimeEffect::amount)
    ).apply(instance, BowChargeTimeEffect::new));
}