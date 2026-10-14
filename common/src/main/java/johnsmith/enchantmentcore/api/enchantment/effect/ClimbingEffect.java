package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.block.Block;

/**
 * Data-driven enchantment effect component that enables a {@link net.minecraft.world.entity.LivingEntity}
 * to climb vertical block surfaces similar to a spider or ladder.
 *
 * @param speed         The {@link LevelBasedValue} scaling the upward climbing velocity (relative to a 0.2 base speed).
 * @param holdOnCrouch  Whether holding the crouch/sneak key suspends the entity in place mid-air against a valid wall.
 * @param allowedBlocks An optional {@link HolderSet} restricting climbing to specific blocks; if empty, any motion-blocking block is climbable.
 */
public record ClimbingEffect(
        LevelBasedValue speed,
        boolean holdOnCrouch,
        Optional<HolderSet<Block>> allowedBlocks
) {
    /** JSON field key for the climbing speed multiplier. */
    public static final String SPEED = "speed";

    /** JSON field key for toggling crouch-to-hold behavior on walls. */
    public static final String HOLD_ON_CROUCH = "hold_on_crouch";

    /** JSON field key for the optional set of climbable blocks. */
    public static final String ALLOWED_BLOCKS = "allowed_blocks";

    /** Registry path identifier for this effect component. */
    public static final String KEY = "climbing";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(SPEED);

    /**
     * Codec for serializing and deserializing {@link ClimbingEffect} instances from JSON.
     */
    public static final Codec<ClimbingEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.optionalFieldOf(SPEED, LevelBasedValue.constant(1.0F)).forGetter(ClimbingEffect::speed),
            Codec.BOOL.optionalFieldOf(HOLD_ON_CROUCH, false).forGetter(ClimbingEffect::holdOnCrouch),
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf(ALLOWED_BLOCKS).forGetter(ClimbingEffect::allowedBlocks)
    ).apply(instance, ClimbingEffect::new));

    // region Convenience Constructors

    /**
     * Constructs a {@link ClimbingEffect} restricted to a specific set of climbable blocks.
     *
     * @param speed         The climbing speed multiplier.
     * @param holdOnCrouch  Whether crouching holds position on the wall.
     * @param allowedBlocks The set of blocks eligible for climbing.
     */
    public ClimbingEffect(LevelBasedValue speed, boolean holdOnCrouch, HolderSet<Block> allowedBlocks) {
        this(speed, holdOnCrouch, Optional.of(allowedBlocks));
    }

    /**
     * Constructs a {@link ClimbingEffect} that permits climbing on any solid motion-blocking surface.
     *
     * @param speed        The climbing speed multiplier.
     * @param holdOnCrouch Whether crouching holds position on the wall.
     */
    public ClimbingEffect(LevelBasedValue speed, boolean holdOnCrouch) {
        this(speed, holdOnCrouch, Optional.empty());
    }
    // endregion
}