package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Data-driven enchantment effect component that controls the visual rendering opacity of enchanted items,
 * armor pieces, Elytra, shields, and tridents, as well as optionally reducing mob armor-visibility detection.
 *
 * @param alphaMultiplier     The {@link LevelBasedValue} alpha opacity multiplier (from {@code 0.0} fully invisible
 *                            to {@code 1.0} fully opaque).
 * @param detectionMitigation An optional {@link LevelBasedValue} (from {@code 0.0} to {@code 1.0}) reducing how much
 *                            an equipped armor piece contributes to {@link net.minecraft.world.entity.LivingEntity#getArmorCoverPercentage()}.
 */
public record TransparencyEffect(
        LevelBasedValue alphaMultiplier,
        Optional<LevelBasedValue> detectionMitigation
) {
    /** JSON field key for the visual alpha opacity multiplier. */
    public static final String ALPHA_MULTIPLIER = "alpha_multiplier";

    /** JSON field key for the mob detection mitigation factor. */
    public static final String DETECTION_MITIGATION = "detection_mitigation";

    /** Registry path identifier for this effect component. */
    public static final String KEY = "transparency";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(ALPHA_MULTIPLIER, DETECTION_MITIGATION);

    /**
     * Codec for serializing and deserializing {@link TransparencyEffect} instances from JSON.
     */
    public static final Codec<TransparencyEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.optionalFieldOf(ALPHA_MULTIPLIER, LevelBasedValue.constant(0.0F)).forGetter(TransparencyEffect::alphaMultiplier),
            LevelBasedValue.CODEC.optionalFieldOf(DETECTION_MITIGATION).forGetter(TransparencyEffect::detectionMitigation)
    ).apply(instance, TransparencyEffect::new));

    // region Convenience Constructors

    /**
     * Constructs a {@link TransparencyEffect} with both visual alpha scaling and mob detection mitigation.
     *
     * @param alphaMultiplier     The visual alpha opacity multiplier.
     * @param detectionMitigation The armor visibility detection mitigation factor.
     */
    public TransparencyEffect(LevelBasedValue alphaMultiplier, LevelBasedValue detectionMitigation) {
        this(alphaMultiplier, Optional.of(detectionMitigation));
    }

    /**
     * Constructs a purely visual {@link TransparencyEffect} without mob detection mitigation.
     *
     * @param alphaMultiplier The visual alpha opacity multiplier.
     */
    public TransparencyEffect(LevelBasedValue alphaMultiplier) {
        this(alphaMultiplier, Optional.empty());
    }
    // endregion
}