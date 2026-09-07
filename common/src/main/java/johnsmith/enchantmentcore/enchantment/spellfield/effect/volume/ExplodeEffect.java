package johnsmith.enchantmentcore.enchantment.spellfield.effect.volume;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVolumeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.effect.ExplosionDefinition;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.ProbabilityDistribution;
import johnsmith.enchantmentcore.util.SpellFieldTaskScheduler;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record ExplodeEffect(
        ExplosionDefinition explosion,
        LevelBasedValue count,
        LevelBasedValue fuseTicks,
        LevelBasedValue fuseVariance,
        LevelBasedValue spatialDelay,
        Optional<ProbabilityDistribution> distribution,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect, SpellFieldVolumeEffect {

    public static final String EXPLOSION = "explosion";
    public static final String COUNT = "count";
    public static final String FUSE_TICKS = "fuse_ticks";
    public static final String FUSE_VARIANCE = "fuse_variance";
    public static final String SPATIAL_DELAY = "spatial_delay";
    public static final String DISTRIBUTION = "distribution";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "explode";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> {
        Set<String> set = new HashSet<>(Set.of(COUNT, FUSE_TICKS, FUSE_VARIANCE, SPATIAL_DELAY));
        set.addAll(ExplosionDefinition.KEY_PROVIDER.getLevelBasedKeys());
        return set;
    };

    public static final MapCodec<ExplodeEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExplosionDefinition.CODEC.fieldOf(EXPLOSION).forGetter(ExplodeEffect::explosion),
            LevelBasedValue.CODEC.optionalFieldOf(COUNT, LevelBasedValue.constant(1.0F)).forGetter(ExplodeEffect::count),
            LevelBasedValue.CODEC.optionalFieldOf(FUSE_TICKS, LevelBasedValue.constant(0.0F)).forGetter(ExplodeEffect::fuseTicks),
            LevelBasedValue.CODEC.optionalFieldOf(FUSE_VARIANCE, LevelBasedValue.constant(0.0F)).forGetter(ExplodeEffect::fuseVariance),
            LevelBasedValue.CODEC.optionalFieldOf(SPATIAL_DELAY, LevelBasedValue.constant(0.0F)).forGetter(ExplodeEffect::spatialDelay),
            ProbabilityDistribution.CODEC.optionalFieldOf(DISTRIBUTION).forGetter(ExplodeEffect::distribution),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(ExplodeEffect::entityFilter)
    ).apply(instance, ExplodeEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;
        this.dispatchExplosion(level, enchantmentLevel, context.owner(), victim.position(), epicenter, volumeScalar);
    }

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, List<GlobalVolume> volumes) {
        if (volumes.isEmpty()) return;

        int totalExplosions = (int) this.count.calculate(enchantmentLevel);
        for (int i = 0; i < totalExplosions; i++) {
            Vec3 targetPoint = this.distribution
                    .map(d -> d.samplePoint(level, enchantmentLevel, spatialReference, epicenter, volumes))
                    .orElseGet(() -> ProbabilityDistribution.getUniformPoint(volumes, level.random));

            this.dispatchExplosion(level, enchantmentLevel, context.owner(), targetPoint, epicenter, 1.0F);
        }
    }

    private void dispatchExplosion(ServerLevel level, int enchantmentLevel, Entity owner, Vec3 targetPos, Vec3 epicenter, float scalar) {
        if (scalar <= 0.0001F) return;

        int baseFuse = (int) this.fuseTicks.calculate(enchantmentLevel);
        int variance = (int) this.fuseVariance.calculate(enchantmentLevel);

        float distance = (float) targetPos.distanceTo(epicenter);
        int spatialOffset = (int) (distance * this.spatialDelay.calculate(enchantmentLevel));

        int fuse = baseFuse + spatialOffset + (variance > 0 ? level.random.nextInt(variance * 2) - variance : 0);
        fuse = Math.max(0, fuse);

        Runnable explodeAction = () -> this.explosion.explode(level, enchantmentLevel, owner, targetPos, scalar);

        SpellFieldTaskScheduler.schedule(level, fuse, explodeAction);
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() { return CODEC; }
}