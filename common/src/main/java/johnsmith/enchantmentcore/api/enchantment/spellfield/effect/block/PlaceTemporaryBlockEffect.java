package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.TopologyEvaluator;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.util.TransientBlockTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldBlockEffect} that temporarily places a configured {@link Block} at target coordinates
 * and schedules automatic restoration via {@link TransientBlockTracker}.
 * <p>
 * Safely handles repeated applications from persistent spell field anchors and chunk unload/reload cycles
 * by verifying that the target chunk is actively ticking blocks and by consulting {@link TransientBlockTracker}
 * to preserve the true pre-spell {@link BlockState} and {@link BlockEntity} metadata.
 *
 * @param block           The temporary {@link Block} to place in the world.
 * @param lifetime        The {@link LevelBasedValue} duration (in ticks) the temporary block remains stable before decaying.
 * @param decayDuration   The {@link LevelBasedValue} duration (in ticks) of the block-breaking crack animation prior to restoration.
 * @param requireAir      Whether placement should be restricted to replaceable blocks (such as air, grass, or fluids).
 * @param restorePrevious Whether the original block state should be restored upon expiration (if {@code false}, restores air).
 * @param evaluator       The {@link TopologyEvaluator} governing deterministic threshold or probabilistic block selection.
 */
public record PlaceTemporaryBlockEffect(
        Block block,
        LevelBasedValue lifetime,
        LevelBasedValue decayDuration,
        boolean requireAir,
        boolean restorePrevious,
        TopologyEvaluator evaluator
) implements SpellFieldBlockEffect {
    /** JSON field key for the temporary block to place. */
    public static final String BLOCK = "block";

    /** JSON field key for the stable lifetime duration in ticks. */
    public static final String LIFETIME = "lifetime";

    /** JSON field key for the visual crack decay duration in ticks. */
    public static final String DECAY_DURATION = "decay_duration";

    /** JSON field key for restricting placement to replaceable blocks. */
    public static final String REQUIRE_AIR = "require_air";

    /** JSON field key for toggling restoration of the prior block state versus air. */
    public static final String RESTORE_PREVIOUS = "restore_previous";

    /** JSON field key for the topological threshold/probability evaluator. */
    public static final String EVALUATOR = "evaluator";

    /** Registry path identifier for this spell field block effect. */
    public static final String KEY = "place_temporary_block";

    /**
     * Supplies the JSON property keys that hold {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> {
        Set<String> set = new HashSet<>(Set.of(LIFETIME, DECAY_DURATION));
        set.addAll(TopologyEvaluator.KEY_PROVIDER.getLevelBasedKeys());
        return set;
    };

    /**
     * MapCodec for serializing and deserializing {@link PlaceTemporaryBlockEffect} definitions.
     */
    public static final MapCodec<PlaceTemporaryBlockEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf(BLOCK).forGetter(PlaceTemporaryBlockEffect::block),
            LevelBasedValue.CODEC.optionalFieldOf(LIFETIME, LevelBasedValue.constant(20)).forGetter(PlaceTemporaryBlockEffect::lifetime),
            LevelBasedValue.CODEC.optionalFieldOf(DECAY_DURATION, LevelBasedValue.constant(20)).forGetter(PlaceTemporaryBlockEffect::decayDuration),
            Codec.BOOL.optionalFieldOf(REQUIRE_AIR, true).forGetter(PlaceTemporaryBlockEffect::requireAir),
            Codec.BOOL.optionalFieldOf(RESTORE_PREVIOUS, true).forGetter(PlaceTemporaryBlockEffect::restorePrevious),
            TopologyEvaluator.CODEC.optionalFieldOf(EVALUATOR, TopologyEvaluator.PROBABILISTIC).forGetter(PlaceTemporaryBlockEffect::evaluator)
    ).apply(instance, PlaceTemporaryBlockEffect::new));

    /**
     * Returns the execution priority of this block effect based on the evaluator's threshold.
     *
     * @param enchantmentLevel The active level of the enchantment.
     * @return The priority scalar.
     */
    @Override
    public float getPriority(int enchantmentLevel) {
        return this.evaluator.getThreshold(enchantmentLevel);
    }

    /**
     * Applies the temporary block placement at the target coordinate and registers restoration with
     * {@link TransientBlockTracker}.
     * <p>
     * Guards against modifying unloaded or non-ticking chunks via {@link ServerLevel#shouldTickBlocksAt(BlockPos)}.
     * If the target coordinate is already tracked by {@link TransientBlockTracker}, the original pre-spell
     * {@link BlockState} and {@link BlockEntity} NBT are retained so that re-application on chunk reload
     * does not overwrite the restoration state with the temporary block itself.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param pos              The target block coordinate.
     * @param scalar           The active topological multiplier for the target.
     * @param modifiedBlocks   A mutable set tracking all block coordinates modified during this execution cycle.
     */
    @Override
    public void apply(
            ServerLevel level,
            int enchantmentLevel,
            EnchantedItemInUse context,
            Entity spatialReference,
            Vec3 epicenter,
            BlockPos pos,
            float scalar,
            Set<BlockPos> modifiedBlocks
    ) {
        if (modifiedBlocks.contains(pos)) return;
        if (!this.evaluator.evaluate(scalar, enchantmentLevel, level.random)) return;

        BlockState originalState = level.getBlockState(pos);
        if (this.requireAir && !originalState.canBeReplaced()) return;

        int stableTicks = (int) this.lifetime.calculate(enchantmentLevel);
        int decayTicks = (int) this.decayDuration.calculate(enchantmentLevel);
        if (stableTicks + decayTicks <= 0) return;

        CompoundTag beTag = null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            beTag = be.saveWithFullMetadata(level.registryAccess());
            if (be instanceof Clearable clearable) {
                clearable.clearContent();
            }
        }

        level.setBlockAndUpdate(pos, this.block.defaultBlockState());
        modifiedBlocks.add(pos);

        long currentTick = level.getGameTime();
        long decayStartTick = currentTick + Math.max(0, stableTicks);
        long expiryTick = decayStartTick + Math.max(0, decayTicks);
        BlockState stateToRestore = this.restorePrevious ? originalState : Blocks.AIR.defaultBlockState();
        TransientBlockTracker.get(level).addBlock(level, pos.immutable(), stateToRestore, beTag, decayStartTick, expiryTick);
    }

    @Override
    public MapCodec<? extends SpellFieldBlockEffect> codec() {
        return CODEC;
    }

    /**
     * Constructs a {@link PlaceTemporaryBlockEffect} using the default {@link TopologyEvaluator#PROBABILISTIC} evaluator.
     *
     * @param block           The temporary block to place.
     * @param lifetime        The stable duration in ticks before decay begins.
     * @param decayDuration   The crack animation duration in ticks before restoration.
     * @param requireAir      Whether placement requires a replaceable existing block state.
     * @param restorePrevious Whether to restore the previous block state rather than air upon expiration.
     */
    public PlaceTemporaryBlockEffect(Block block, LevelBasedValue lifetime, LevelBasedValue decayDuration, boolean requireAir, boolean restorePrevious) {
        this(block, lifetime, decayDuration, requireAir, restorePrevious, TopologyEvaluator.PROBABILISTIC);
    }
}