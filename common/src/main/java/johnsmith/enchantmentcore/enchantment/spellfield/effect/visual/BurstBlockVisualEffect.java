package johnsmith.enchantmentcore.enchantment.spellfield.effect.visual;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.math.TopologyEvaluator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record BurstBlockVisualEffect(
        ParticleOptions particle,
        LevelBasedValue count,
        LevelBasedValue speed,
        TopologyEvaluator evaluator
) implements SpellFieldBlockEffect {

    public static final String PARTICLE = "particle";
    public static final String COUNT = "count";
    public static final String SPEED = "speed";
    public static final String EVALUATOR = "evaluator";

    public static final String KEY = "burst_block";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(COUNT, SPEED);

    public static final MapCodec<BurstBlockVisualEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ParticleTypes.CODEC.fieldOf(PARTICLE).forGetter(BurstBlockVisualEffect::particle),
            LevelBasedValue.CODEC.fieldOf(COUNT).forGetter(BurstBlockVisualEffect::count),
            LevelBasedValue.CODEC.fieldOf(SPEED).forGetter(BurstBlockVisualEffect::speed),
            TopologyEvaluator.CODEC.optionalFieldOf(EVALUATOR, TopologyEvaluator.PROBABILISTIC).forGetter(BurstBlockVisualEffect::evaluator)
    ).apply(instance, BurstBlockVisualEffect::new));

    @Override
    public float getPriority(int enchantmentLevel) {
        return this.evaluator.getThreshold(enchantmentLevel);
    }

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, BlockPos pos, float scalar, Set<BlockPos> modifiedBlocks) {
        if (!this.evaluator.evaluate(scalar, enchantmentLevel, level.random)) return;

        int particleCount = (int) (this.count.calculate(enchantmentLevel) * scalar);
        if (particleCount <= 0) return;

        float particleSpeed = this.speed.calculate(enchantmentLevel) * scalar;
        Vec3 center = Vec3.atCenterOf(pos);

        level.sendParticles(this.particle, center.x, center.y, center.z, particleCount, 0.5, 0.5, 0.5, particleSpeed);
    }

    @Override
    public MapCodec<? extends SpellFieldBlockEffect> codec() { return CODEC; }
}