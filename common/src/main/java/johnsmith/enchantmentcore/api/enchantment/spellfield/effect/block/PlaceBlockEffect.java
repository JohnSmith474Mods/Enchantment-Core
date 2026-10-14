package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.TopologyEvaluator;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldBlockEffect} that permanently places the default state of a configured {@link Block}
 * at evaluated coordinates within a spell field volume.
 *
 * @param block      The {@link Block} whose default block state is placed.
 * @param requireAir If {@code true}, placement only occurs at coordinates where {@link net.minecraft.world.level.block.state.BlockState#canBeReplaced()} is {@code true}.
 * @param evaluator  The {@link TopologyEvaluator} governing deterministic threshold or probabilistic activation.
 */
public record PlaceBlockEffect(
        Block block,
        boolean requireAir,
        TopologyEvaluator evaluator
) implements SpellFieldBlockEffect {
    /** JSON field key for the block to place. */
    public static final String BLOCK = "block";

    /** JSON field key for restricting placement to replaceable blocks. */
    public static final String REQUIRE_AIR = "require_air";

    /** JSON field key for the topological threshold/probability evaluator. */
    public static final String EVALUATOR = "evaluator";

    /** Registry path identifier for this spell field block effect. */
    public static final String KEY = "place_block";

    /**
     * Supplies the JSON property keys holding {@link net.minecraft.world.item.enchantment.LevelBasedValue}
     * definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = TopologyEvaluator.KEY_PROVIDER;

    /**
     * MapCodec for serializing and deserializing {@link PlaceBlockEffect} instances from JSON.
     */
    public static final MapCodec<PlaceBlockEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf(BLOCK).forGetter(PlaceBlockEffect::block),
            Codec.BOOL.optionalFieldOf(REQUIRE_AIR, true).forGetter(PlaceBlockEffect::requireAir),
            TopologyEvaluator.CODEC.optionalFieldOf(EVALUATOR, TopologyEvaluator.PROBABILISTIC).forGetter(PlaceBlockEffect::evaluator)
    ).apply(instance, PlaceBlockEffect::new));

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
     * Places the configured block's default state at {@code pos} if the coordinate has not yet been
     * modified during this cycle, passes the {@link #evaluator()}, and satisfies {@link #requireAir()}.
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
        if (this.requireAir && !level.getBlockState(pos).canBeReplaced()) return;

        level.setBlockAndUpdate(pos, this.block.defaultBlockState());
        modifiedBlocks.add(pos);
    }

    @Override
    public MapCodec<? extends SpellFieldBlockEffect> codec() { return CODEC; }

    /**
     * Constructs a {@link PlaceBlockEffect} using the default {@link TopologyEvaluator#PROBABILISTIC} evaluator.
     *
     * @param block      The block to place.
     * @param requireAir Whether placement requires a replaceable existing block state.
     */
    public PlaceBlockEffect(Block block, boolean requireAir) {
        this(block, requireAir, TopologyEvaluator.PROBABILISTIC);
    }
}