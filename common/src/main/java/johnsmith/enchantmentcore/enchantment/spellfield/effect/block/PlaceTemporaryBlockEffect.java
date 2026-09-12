package johnsmith.enchantmentcore.enchantment.spellfield.effect.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.math.TopologyEvaluator;
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

public record PlaceTemporaryBlockEffect(
        Block block,
        LevelBasedValue lifetime,
        LevelBasedValue decayDuration,
        boolean requireAir,
        boolean restorePrevious,
        TopologyEvaluator evaluator
) implements SpellFieldBlockEffect {

    public static final String BLOCK = "block";
    public static final String LIFETIME = "lifetime";
    public static final String DECAY_DURATION = "decay_duration";
    public static final String REQUIRE_AIR = "require_air";
    public static final String RESTORE_PREVIOUS = "restore_previous";
    public static final String EVALUATOR = "evaluator";

    public static final String KEY = "place_temporary_block";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> {
        Set<String> set = new HashSet<>(Set.of(LIFETIME, DECAY_DURATION));
        set.addAll(TopologyEvaluator.KEY_PROVIDER.getLevelBasedKeys());
        return set;
    };

    public static final MapCodec<PlaceTemporaryBlockEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf(BLOCK).forGetter(PlaceTemporaryBlockEffect::block),
            LevelBasedValue.CODEC.optionalFieldOf(LIFETIME, LevelBasedValue.constant(20)).forGetter(PlaceTemporaryBlockEffect::lifetime),
            LevelBasedValue.CODEC.optionalFieldOf(DECAY_DURATION, LevelBasedValue.constant(20)).forGetter(PlaceTemporaryBlockEffect::decayDuration),
            Codec.BOOL.optionalFieldOf(REQUIRE_AIR, true).forGetter(PlaceTemporaryBlockEffect::requireAir),
            Codec.BOOL.optionalFieldOf(RESTORE_PREVIOUS, true).forGetter(PlaceTemporaryBlockEffect::restorePrevious),
            TopologyEvaluator.CODEC.optionalFieldOf(EVALUATOR, TopologyEvaluator.PROBABILISTIC).forGetter(PlaceTemporaryBlockEffect::evaluator)
    ).apply(instance, PlaceTemporaryBlockEffect::new));

    @Override
    public float getPriority(int enchantmentLevel) {
        return this.evaluator.getThreshold(enchantmentLevel);
    }

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, BlockPos pos, float scalar, Set<BlockPos> modifiedBlocks) {
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
}