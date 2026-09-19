package johnsmith.enchantmentcore.enchantment.spellfield.effect.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.math.TopologyEvaluator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record IgniteBlockEffect(
        TopologyEvaluator evaluator
) implements SpellFieldBlockEffect {

    public static final String EVALUATOR = "evaluator";

    public static final String KEY = "ignite_block";

    public static final LevelBasedKeyProvider KEY_PROVIDER = TopologyEvaluator.KEY_PROVIDER;

    public static final MapCodec<IgniteBlockEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TopologyEvaluator.CODEC.optionalFieldOf(EVALUATOR, TopologyEvaluator.PROBABILISTIC).forGetter(IgniteBlockEffect::evaluator)
    ).apply(instance, IgniteBlockEffect::new));

    @Override
    public float getPriority(int enchantmentLevel) {
        return this.evaluator.getThreshold(enchantmentLevel);
    }

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, BlockPos pos, float scalar, Set<BlockPos> modifiedBlocks) {
        if (modifiedBlocks.contains(pos)) return;
        if (!this.evaluator.evaluate(scalar, enchantmentLevel, level.random)) return;

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            BlockState stateBelow = level.getBlockState(pos.below());
            if (stateBelow.isSolidRender(level, pos.below())) {
                level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
                modifiedBlocks.add(pos);
            }
        }
    }

    @Override
    public MapCodec<? extends SpellFieldBlockEffect> codec() {
        return CODEC;
    }
}