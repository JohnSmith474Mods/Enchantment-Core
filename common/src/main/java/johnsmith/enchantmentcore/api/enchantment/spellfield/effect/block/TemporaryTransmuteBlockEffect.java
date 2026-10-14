package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.TopologyEvaluator;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.util.TransientBlockTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldBlockEffect} that temporarily transmutes existing non-air blocks into a replacement
 * {@link Block} and schedules automatic restoration of the original block state and {@link BlockEntity} data.
 * <p>
 * Prevents permanent griefing across chunk unloads and reloads by verifying that the target chunk is actively
 * ticking blocks and by querying {@link TransientBlockTracker#getOriginalState(BlockPos)} before reading
 * from the world, ensuring repeated anchor ticks never mistake the transmuted block for the original block.
 *
 * @param targetBlocks     An optional {@link HolderSet} whitelist of blocks eligible for transmutation.
 * @param replacementBlock The temporary replacement {@link Block} to place.
 * @param lifetime         The {@link LevelBasedValue} duration (in ticks) the transmuted block remains stable before decaying.
 * @param decayDuration    The {@link LevelBasedValue} duration (in ticks) of the block-breaking crack animation prior to restoration.
 * @param evaluator        The {@link TopologyEvaluator} governing deterministic threshold or probabilistic block selection.
 */
public record TemporaryTransmuteBlockEffect(
        Optional<HolderSet<Block>> targetBlocks,
        Block replacementBlock,
        LevelBasedValue lifetime,
        LevelBasedValue decayDuration,
        TopologyEvaluator evaluator
) implements SpellFieldBlockEffect {
    /** JSON field key for the optional whitelist of eligible target blocks. */
    public static final String TARGET_BLOCKS = "target_blocks";

    /** JSON field key for the temporary replacement block. */
    public static final String REPLACEMENT_BLOCK = "replacement_block";

    /** JSON field key for the stable lifetime duration in ticks. */
    public static final String LIFETIME = "lifetime";

    /** JSON field key for the visual crack decay duration in ticks. */
    public static final String DECAY_DURATION = "decay_duration";

    /** JSON field key for the topological threshold/probability evaluator. */
    public static final String EVALUATOR = "evaluator";

    /** Registry path identifier for this spell field block effect. */
    public static final String KEY = "temporary_transmute_block";

    /**
     * Supplies the JSON property keys that hold {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> {
        Set<String> set = new HashSet<>(Set.of(LIFETIME, DECAY_DURATION));
        set.addAll(TopologyEvaluator.KEY_PROVIDER.getLevelBasedKeys());
        return set;
    };

    /**
     * MapCodec for serializing and deserializing {@link TemporaryTransmuteBlockEffect} definitions.
     */
    public static final MapCodec<TemporaryTransmuteBlockEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf(TARGET_BLOCKS).forGetter(TemporaryTransmuteBlockEffect::targetBlocks),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf(REPLACEMENT_BLOCK).forGetter(TemporaryTransmuteBlockEffect::replacementBlock),
            LevelBasedValue.CODEC.optionalFieldOf(LIFETIME, LevelBasedValue.constant(20)).forGetter(TemporaryTransmuteBlockEffect::lifetime),
            LevelBasedValue.CODEC.optionalFieldOf(DECAY_DURATION, LevelBasedValue.constant(20)).forGetter(TemporaryTransmuteBlockEffect::decayDuration),
            TopologyEvaluator.CODEC.optionalFieldOf(EVALUATOR, TopologyEvaluator.PROBABILISTIC).forGetter(TemporaryTransmuteBlockEffect::evaluator)
    ).apply(instance, TemporaryTransmuteBlockEffect::new));

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
     * Executes the temporary block transmutation at the specified coordinate and schedules restoration
     * with {@link TransientBlockTracker}.
     * <p>
     * Skips coordinates in unloaded or non-ticking chunks via {@link ServerLevel#shouldTickBlocksAt(BlockPos)}.
     * Evaluates target block predicates against the true original {@link BlockState} (retrieved from
     * {@link TransientBlockTracker#getOriginalState(BlockPos)} if already transmuted) so that persistent
     * spell fields can refresh transmutation timers without clobbering the original block state or NBT.
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
        if (!level.shouldTickBlocksAt(pos)) return;
        if (!this.evaluator.evaluate(scalar, enchantmentLevel, level.random)) return;

        TransientBlockTracker tracker = TransientBlockTracker.get(level);
        BlockState trackedOriginal = tracker.getOriginalState(pos);
        BlockState effectiveOriginal = trackedOriginal != null ? trackedOriginal : level.getBlockState(pos);

        if (effectiveOriginal.isAir() || effectiveOriginal.getDestroySpeed(level, pos) < 0.0F) return;
        if (this.targetBlocks.isPresent() && !effectiveOriginal.is(this.targetBlocks.get())) return;

        int stableTicks = Math.max(0, (int) this.lifetime.calculate(enchantmentLevel));
        int decayTicks = Math.max(0, (int) this.decayDuration.calculate(enchantmentLevel));
        if (stableTicks + decayTicks <= 0) return;

        CompoundTag beTag = null;
        // Only capture and clear the BlockEntity on initial transmutation, not when refreshing an already-tracked block.
        if (trackedOriginal == null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                beTag = be.saveWithFullMetadata(level.registryAccess());
                Clearable.tryClear(be);
            }
        }

        level.setBlockAndUpdate(pos, this.replacementBlock.defaultBlockState());
        modifiedBlocks.add(pos);

        tracker.addBlock(level, pos.immutable(), effectiveOriginal, beTag, stableTicks, decayTicks);
    }

    @Override
    public MapCodec<? extends SpellFieldBlockEffect> codec() {
        return CODEC;
    }

    /**
     * Constructs a {@link TemporaryTransmuteBlockEffect} restricted to a specific target block set using
     * the default {@link TopologyEvaluator#PROBABILISTIC} evaluator.
     *
     * @param targetBlocks     The whitelist of blocks eligible for transmutation.
     * @param replacementBlock The temporary replacement block.
     * @param lifetime         The stable duration in ticks before decay begins.
     * @param decayDuration    The crack animation duration in ticks before restoration.
     */
    public TemporaryTransmuteBlockEffect(HolderSet<Block> targetBlocks, Block replacementBlock, LevelBasedValue lifetime, LevelBasedValue decayDuration) {
        this(Optional.of(targetBlocks), replacementBlock, lifetime, decayDuration, TopologyEvaluator.PROBABILISTIC);
    }

    /**
     * Constructs an unrestricted {@link TemporaryTransmuteBlockEffect} that can transmute any breakable
     * non-air block using the default {@link TopologyEvaluator#PROBABILISTIC} evaluator.
     *
     * @param replacementBlock The temporary replacement block.
     * @param lifetime         The stable duration in ticks before decay begins.
     * @param decayDuration    The crack animation duration in ticks before restoration.
     */
    public TemporaryTransmuteBlockEffect(Block replacementBlock, LevelBasedValue lifetime, LevelBasedValue decayDuration) {
        this(Optional.empty(), replacementBlock, lifetime, decayDuration, TopologyEvaluator.PROBABILISTIC);
    }
}