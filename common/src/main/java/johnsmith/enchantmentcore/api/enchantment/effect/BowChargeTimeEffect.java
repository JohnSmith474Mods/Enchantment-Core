package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Data-driven enchantment effect component that modifies the draw time required for a {@link net.minecraft.world.item.BowItem}
 * to reach full power.
 * <p>
 * Applied on both the server (via {@code BowItemMixin} during arrow release) and the client (via
 * {@code ItemPropertiesMixin} for pull animation synchronization).
 *
 * @param amount A {@link LevelBasedValue} calculating the change in full-charge duration in seconds
 *               (negative values reduce draw time).
 */
public record BowChargeTimeEffect(
        LevelBasedValue amount
) {
    /** JSON field key for the charge time modification in seconds. */
    public static final String AMOUNT = "amount";

    /** Registry path identifier for this effect component. */
    public static final String KEY = "bow_charge_time";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(AMOUNT);

    /**
     * Codec for serializing and deserializing {@link BowChargeTimeEffect} instances from JSON.
     */
    public static final Codec<BowChargeTimeEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(AMOUNT).forGetter(BowChargeTimeEffect::amount)
    ).apply(instance, BowChargeTimeEffect::new));
}