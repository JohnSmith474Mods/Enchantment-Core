package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.TopologyEvaluator;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldBlockEffect} that permanently replaces existing breakable non-air blocks with
 * the default state of a configured {@link #replacementBlock()}.
 *
 * @param targetBlocks     An optional {@link HolderSet} whitelist restricting which blocks can be transmuted.
 * @param replacementBlock The {@link Block} whose default state replaces matching target blocks.
 * @param evaluator        The {@link TopologyEvaluator} governing deterministic threshold or probabilistic activation.
 */
public record TransmuteBlockEffect(
        Optional<HolderSet<Block>> targetBlocks,
        Block replacementBlock,
        TopologyEvaluator evaluator
) implements SpellFieldBlockEffect {
    /** JSON field key for the optional whitelist of eligible target blocks. */
    public static final String TARGET_BLOCKS = "target_blocks";

    /** JSON field key for the replacement block. */
    public static final String REPLACEMENT_BLOCK = "replacement_block";

    /** JSON field key for the topological threshold/probability evaluator. */
    public static final String EVALUATOR = "evaluator";

    /** Registry path identifier for this spell field block effect. */
    public static final String KEY = "transmute_block";

    /**
     * Supplies the JSON property keys holding {@link net.minecraft.world.item.enchantment.LevelBasedValue}
     * definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = TopologyEvaluator.KEY_PROVIDER;

    /**
     * MapCodec for serializing and deserializing {@link TransmuteBlockEffect} instances from JSON.
     */
    public static final MapCodec<TransmuteBlockEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf(TARGET_BLOCKS).forGetter(TransmuteBlockEffect::targetBlocks),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf(REPLACEMENT_BLOCK).forGetter(TransmuteBlockEffect::replacementBlock),
            TopologyEvaluator.CODEC.optionalFieldOf(EVALUATOR, TopologyEvaluator.PROBABILISTIC).forGetter(TransmuteBlockEffect::evaluator)
    ).apply(instance, TransmuteBlockEffect::new));

    /**
     * Returns the execution priority of this block effect derived from the evaluator's threshold.
     *
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @return The priority scalar.
     */
    @Override
    public float getPriority(int enchantmentLevel) {
        return this.evaluator.getThreshold(enchantmentLevel);
    }

    /**
     * Replaces the block at {@code pos} with {@link #replacementBlock()}'s default state if the coordinate
     * has not yet been modified during this cycle, passes the {@link #evaluator()}, is non-air and breakable,
     * and matches {@link #targetBlocks()} (if present).
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param pos              The target block coordinate.
     * @param scalar           The active topological multiplier at {@code pos}.
     * @param modifiedBlocks   A mutable set tracking block coordinates modified during this evaluation cycle.
     */
    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, BlockPos pos, float scalar, Set<BlockPos> modifiedBlocks) {
        if (modifiedBlocks.contains(pos)) return;
        if (!this.evaluator.evaluate(scalar, enchantmentLevel, level.getRandom())) return;

        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) return;
        if (this.targetBlocks.isPresent() && !state.is(this.targetBlocks.get())) return;

        level.setBlockAndUpdate(pos, this.replacementBlock.defaultBlockState());
        modifiedBlocks.add(pos);
    }

    @Override
    public MapCodec<? extends SpellFieldBlockEffect> codec() {
        return CODEC;
    }

    /**
     * Constructs a {@link TransmuteBlockEffect} restricted to a specific target block set using the
     * default {@link TopologyEvaluator#PROBABILISTIC} evaluator.
     *
     * @param targetBlocks     The whitelist of blocks eligible for transmutation.
     * @param replacementBlock The replacement block to place.
     */
    public TransmuteBlockEffect(HolderSet<Block> targetBlocks, Block replacementBlock) {
        this(Optional.of(targetBlocks), replacementBlock, TopologyEvaluator.PROBABILISTIC);
    }

    /**
     * Constructs an unrestricted {@link TransmuteBlockEffect} that transmutes any breakable non-air block
     * using the default {@link TopologyEvaluator#PROBABILISTIC} evaluator.
     *
     * @param replacementBlock The replacement block to place.
     */
    public TransmuteBlockEffect(Block replacementBlock) {
        this(Optional.empty(), replacementBlock, TopologyEvaluator.PROBABILISTIC);
    }
}