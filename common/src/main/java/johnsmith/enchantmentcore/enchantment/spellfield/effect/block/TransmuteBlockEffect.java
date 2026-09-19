package johnsmith.enchantmentcore.enchantment.spellfield.effect.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.math.TopologyEvaluator;

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

public record TransmuteBlockEffect(
        Optional<HolderSet<Block>> targetBlocks,
        Block replacementBlock,
        TopologyEvaluator evaluator
) implements SpellFieldBlockEffect {

    public static final String TARGET_BLOCKS = "target_blocks";
    public static final String REPLACEMENT_BLOCK = "replacement_block";
    public static final String EVALUATOR = "evaluator";

    public static final String KEY = "transmute_block";

    public static final LevelBasedKeyProvider KEY_PROVIDER = TopologyEvaluator.KEY_PROVIDER;

    public static final MapCodec<TransmuteBlockEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf(TARGET_BLOCKS).forGetter(TransmuteBlockEffect::targetBlocks),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf(REPLACEMENT_BLOCK).forGetter(TransmuteBlockEffect::replacementBlock),
            TopologyEvaluator.CODEC.optionalFieldOf(EVALUATOR, TopologyEvaluator.PROBABILISTIC).forGetter(TransmuteBlockEffect::evaluator)
    ).apply(instance, TransmuteBlockEffect::new));

    @Override
    public float getPriority(int enchantmentLevel) {
        return this.evaluator.getThreshold(enchantmentLevel);
    }

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, BlockPos pos, float scalar, Set<BlockPos> modifiedBlocks) {
        if (modifiedBlocks.contains(pos)) return;
        if (!this.evaluator.evaluate(scalar, enchantmentLevel, level.random)) return;

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
}