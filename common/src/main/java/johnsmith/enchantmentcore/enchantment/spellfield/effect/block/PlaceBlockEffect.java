package johnsmith.enchantmentcore.enchantment.spellfield.effect.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.math.TopologyEvaluator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public record PlaceBlockEffect(
        Block block,
        boolean requireAir,
        TopologyEvaluator evaluator
) implements SpellFieldBlockEffect {

    public static final String BLOCK = "block";
    public static final String REQUIRE_AIR = "require_air";
    public static final String EVALUATOR = "evaluator";

    public static final String KEY = "place_block";

    public static final LevelBasedKeyProvider KEY_PROVIDER = TopologyEvaluator.KEY_PROVIDER;

    public static final MapCodec<PlaceBlockEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf(BLOCK).forGetter(PlaceBlockEffect::block),
            Codec.BOOL.optionalFieldOf(REQUIRE_AIR, true).forGetter(PlaceBlockEffect::requireAir),
            TopologyEvaluator.CODEC.optionalFieldOf(EVALUATOR, TopologyEvaluator.PROBABILISTIC).forGetter(PlaceBlockEffect::evaluator)
    ).apply(instance, PlaceBlockEffect::new));

    @Override
    public float getPriority(int enchantmentLevel) {
        return this.evaluator.getThreshold(enchantmentLevel);
    }

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, BlockPos pos, float scalar, Set<BlockPos> modifiedBlocks) {
        if (modifiedBlocks.contains(pos)) return;
        if (!this.evaluator.evaluate(scalar, enchantmentLevel, level.random)) return;

        if (this.requireAir && !level.getBlockState(pos).canBeReplaced()) return;

        level.setBlockAndUpdate(pos, this.block.defaultBlockState());
        modifiedBlocks.add(pos);
    }

    @Override
    public MapCodec<? extends SpellFieldBlockEffect> codec() { return CODEC; }
}