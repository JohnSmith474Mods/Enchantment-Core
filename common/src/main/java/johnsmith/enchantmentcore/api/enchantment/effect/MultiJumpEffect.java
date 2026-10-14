package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;

/**
 * Data-driven enchantment effect component that grants additional mid-air jumps to a
 * {@link net.minecraft.world.entity.LivingEntity} before touching the ground.
 *
 * @param jumps       The {@link EnchantmentValueEffect} calculating the maximum number of mid-air jumps allowed.
 * @param allowElytra Whether mid-air jumps can be executed while actively gliding with an Elytra.
 */
public record MultiJumpEffect(
        EnchantmentValueEffect jumps,
        boolean allowElytra
) {
    /** JSON field key for the mid-air jump count value effect. */
    public static final String JUMPS = "jumps";

    /** JSON field key for permitting multi-jumps during Elytra flight. */
    public static final String ALLOW_ELYTRA = "allow_elytra";

    /** Registry path identifier for this effect component. */
    public static final String KEY = "multi_jump";

    /**
     * Supplies the JSON property keys holding level-based value definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(JUMPS);

    /**
     * Codec for serializing and deserializing {@link MultiJumpEffect} instances from JSON.
     */
    public static final Codec<MultiJumpEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EnchantmentValueEffect.CODEC.fieldOf(JUMPS).forGetter(MultiJumpEffect::jumps),
            Codec.BOOL.optionalFieldOf(ALLOW_ELYTRA, false).forGetter(MultiJumpEffect::allowElytra)
    ).apply(instance, MultiJumpEffect::new));
}