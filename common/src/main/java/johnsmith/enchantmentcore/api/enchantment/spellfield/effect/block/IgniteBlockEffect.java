package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.block;

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
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldBlockEffect} that ignites fire in air blocks resting directly atop solid-render
 * surfaces within a spell field volume when permitted by its {@link TopologyEvaluator}.
 * <p>
 * Automatically selects soul fire or standard fire via {@link BaseFireBlock#getState(net.minecraft.world.level.BlockGetter, BlockPos)}.
 *
 * @param evaluator The {@link TopologyEvaluator} governing deterministic threshold or probabilistic activation.
 */
public record IgniteBlockEffect(
        TopologyEvaluator evaluator
) implements SpellFieldBlockEffect {
    /** JSON field key for the topological threshold/probability evaluator. */
    public static final String EVALUATOR = "evaluator";

    /** Registry path identifier for this spell field block effect. */
    public static final String KEY = "ignite_block";

    /**
     * Supplies the JSON property keys holding {@link net.minecraft.world.item.enchantment.LevelBasedValue}
     * definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = TopologyEvaluator.KEY_PROVIDER;

    /**
     * MapCodec for serializing and deserializing {@link IgniteBlockEffect} instances from JSON.
     */
    public static final MapCodec<IgniteBlockEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TopologyEvaluator.CODEC.optionalFieldOf(EVALUATOR, TopologyEvaluator.PROBABILISTIC).forGetter(IgniteBlockEffect::evaluator)
    ).apply(instance, IgniteBlockEffect::new));

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
     * Places a fire block at {@code pos} if the position is currently air, rests atop a solid-render
     * block, and passes the {@link #evaluator()}.
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
        if (state.isAir()) {
            BlockState stateBelow = level.getBlockState(pos.below());
            if (stateBelow.isSolidRender()) {
                level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
                modifiedBlocks.add(pos);
            }
        }
    }

    @Override
    public MapCodec<? extends SpellFieldBlockEffect> codec() {
        return CODEC;
    }

    /**
     * Constructs an {@link IgniteBlockEffect} using the default {@link TopologyEvaluator#PROBABILISTIC} evaluator.
     */
    public IgniteBlockEffect() {
        this(TopologyEvaluator.PROBABILISTIC);
    }
}