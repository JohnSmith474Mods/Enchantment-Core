package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.TopologyEvaluator;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldBlockEffect} that destroys breakable blocks within a spell field volume
 * when permitted by its {@link TopologyEvaluator}, optionally dropping block loot resources
 * using the triggering enchanted item context.
 * <p>
 * Unbreakable blocks (where {@link BlockState#getDestroySpeed} is negative) and blocks listed in
 * {@link johnsmith.enchantmentcore.config.Config#SPELL_FIELD_BLOCK_BLACKLIST} are never broken.
 *
 * @param dropResources Whether broken blocks should drop their loot table resources as if mined with {@code context.itemStack()}.
 * @param evaluator     The {@link TopologyEvaluator} governing deterministic threshold or probabilistic activation.
 */
public record BreakBlockEffect(
        boolean dropResources,
        TopologyEvaluator evaluator
) implements SpellFieldBlockEffect {
    /** JSON field key for toggling block resource drops. */
    public static final String DROP_RESOURCES = "drop_resources";

    /** JSON field key for the topological threshold/probability evaluator. */
    public static final String EVALUATOR = "evaluator";

    /** Registry path identifier for this spell field block effect. */
    public static final String KEY = "break_block";

    /**
     * Supplies the JSON property keys holding {@link net.minecraft.world.item.enchantment.LevelBasedValue}
     * definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = TopologyEvaluator.KEY_PROVIDER;

    /**
     * MapCodec for serializing and deserializing {@link BreakBlockEffect} instances from JSON.
     */
    public static final MapCodec<BreakBlockEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf(DROP_RESOURCES, true).forGetter(BreakBlockEffect::dropResources),
            TopologyEvaluator.CODEC.optionalFieldOf(EVALUATOR, TopologyEvaluator.PROBABILISTIC).forGetter(BreakBlockEffect::evaluator)
    ).apply(instance, BreakBlockEffect::new));

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
     * Breaks the block at {@code pos} if it has not yet been modified during this cycle, passes the
     * {@link #evaluator()}, and has a non-negative destroy speed.
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
        if (!this.evaluator.evaluate(scalar, enchantmentLevel, level.random)) return;

        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) return;

        if (this.dropResources) {
            BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            Block.dropResources(state, level, pos, blockEntity, context.owner(), context.itemStack());
        }

        level.removeBlock(pos, false);
        modifiedBlocks.add(pos);
    }

    @Override
    public MapCodec<? extends SpellFieldBlockEffect> codec() {
        return CODEC;
    }

    /**
     * Constructs a {@link BreakBlockEffect} using the default {@link TopologyEvaluator#PROBABILISTIC} evaluator.
     *
     * @param dropResources Whether broken blocks should drop their resources.
     */
    public BreakBlockEffect(boolean dropResources) {
        this(dropResources, TopologyEvaluator.PROBABILISTIC);
    }
}