package johnsmith.enchantmentcore.enchantment.spellfield.effect.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.math.TopologyEvaluator;
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

public record TemporaryTransmuteBlockEffect(
        Optional<HolderSet<Block>> targetBlocks,
        Block replacementBlock,
        LevelBasedValue lifetime,
        LevelBasedValue decayDuration,
        TopologyEvaluator evaluator
) implements SpellFieldBlockEffect {

    public static final String TARGET_BLOCKS = "target_blocks";
    public static final String REPLACEMENT_BLOCK = "replacement_block";
    public static final String LIFETIME = "lifetime";
    public static final String DECAY_DURATION = "decay_duration";
    public static final String EVALUATOR = "evaluator";

    public static final String KEY = "temporary_transmute_block";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> {
        Set<String> set = new HashSet<>(Set.of(LIFETIME, DECAY_DURATION));
        set.addAll(TopologyEvaluator.KEY_PROVIDER.getLevelBasedKeys());
        return set;
    };

    public static final MapCodec<TemporaryTransmuteBlockEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf(TARGET_BLOCKS).forGetter(TemporaryTransmuteBlockEffect::targetBlocks),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf(REPLACEMENT_BLOCK).forGetter(TemporaryTransmuteBlockEffect::replacementBlock),
            LevelBasedValue.CODEC.optionalFieldOf(LIFETIME, LevelBasedValue.constant(20)).forGetter(TemporaryTransmuteBlockEffect::lifetime),
            LevelBasedValue.CODEC.optionalFieldOf(DECAY_DURATION, LevelBasedValue.constant(20)).forGetter(TemporaryTransmuteBlockEffect::decayDuration),
            TopologyEvaluator.CODEC.optionalFieldOf(EVALUATOR, TopologyEvaluator.PROBABILISTIC).forGetter(TemporaryTransmuteBlockEffect::evaluator)
    ).apply(instance, TemporaryTransmuteBlockEffect::new));

    @Override
    public float getPriority(int enchantmentLevel) {
        return this.evaluator.getThreshold(enchantmentLevel);
    }

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, BlockPos pos, float scalar, Set<BlockPos> modifiedBlocks) {
        if (modifiedBlocks.contains(pos)) return;
        if (!this.evaluator.evaluate(scalar, enchantmentLevel, level.random)) return;

        BlockState originalState = level.getBlockState(pos);
        if (originalState.isAir() || originalState.getDestroySpeed(level, pos) < 0.0F) return;

        if (this.targetBlocks.isPresent() && !originalState.is(this.targetBlocks.get())) return;

        int stableTicks = (int) this.lifetime.calculate(enchantmentLevel);
        int decayTicks = (int) this.decayDuration.calculate(enchantmentLevel);
        if (stableTicks + decayTicks <= 0) return;

        CompoundTag beTag = null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            beTag = be.saveWithFullMetadata(level.registryAccess());
            Clearable.tryClear(be);
        }

        level.setBlockAndUpdate(pos, this.replacementBlock.defaultBlockState());
        modifiedBlocks.add(pos);

        long currentTick = level.getGameTime();
        long decayStartTick = currentTick + Math.max(0, stableTicks);
        long expiryTick = decayStartTick + Math.max(0, decayTicks);
        TransientBlockTracker.get(level).addBlock(level, pos.immutable(), originalState, beTag, decayStartTick, expiryTick);
    }

    @Override
    public MapCodec<? extends SpellFieldBlockEffect> codec() {
        return CODEC;
    }
}