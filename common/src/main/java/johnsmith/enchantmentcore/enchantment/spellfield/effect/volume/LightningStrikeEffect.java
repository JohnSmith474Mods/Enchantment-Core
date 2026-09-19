package johnsmith.enchantmentcore.enchantment.spellfield.effect.volume;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVolumeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.ProbabilityDistribution;
import johnsmith.enchantmentcore.util.SpellFieldTaskScheduler;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record LightningStrikeEffect(
        LevelBasedValue count,
        LevelBasedValue delayTicks,
        LevelBasedValue delayVariance,
        boolean visualOnly,
        Optional<ProbabilityDistribution> distribution,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect, SpellFieldVolumeEffect {

    public static final String COUNT = "count";
    public static final String DELAY_TICKS = "delay_ticks";
    public static final String DELAY_VARIANCE = "delay_variance";
    public static final String VISUAL_ONLY = "visual_only";
    public static final String DISTRIBUTION = "distribution";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "lightning_strike";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(COUNT, DELAY_TICKS, DELAY_VARIANCE);

    public static final MapCodec<LightningStrikeEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.optionalFieldOf(COUNT, LevelBasedValue.constant(1.0F)).forGetter(LightningStrikeEffect::count),
            LevelBasedValue.CODEC.optionalFieldOf(DELAY_TICKS, LevelBasedValue.constant(0.0F)).forGetter(LightningStrikeEffect::delayTicks),
            LevelBasedValue.CODEC.optionalFieldOf(DELAY_VARIANCE, LevelBasedValue.constant(0.0F)).forGetter(LightningStrikeEffect::delayVariance),
            Codec.BOOL.optionalFieldOf(VISUAL_ONLY, false).forGetter(LightningStrikeEffect::visualOnly),
            ProbabilityDistribution.CODEC.optionalFieldOf(DISTRIBUTION).forGetter(LightningStrikeEffect::distribution),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(LightningStrikeEffect::entityFilter)
    ).apply(instance, LightningStrikeEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;
        this.dispatchStrikes(level, enchantmentLevel, victim.position(), volumeScalar);
    }

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, List<GlobalVolume> volumes) {
        if (volumes.isEmpty()) return;

        Vec3 targetPoint = this.distribution
                .map(d -> d.samplePoint(level, enchantmentLevel, spatialReference, epicenter, volumes))
                .orElseGet(() -> ProbabilityDistribution.getUniformPoint(volumes, level.random));

        this.dispatchStrikes(level, enchantmentLevel, targetPoint, 1.0F);
    }

    private void dispatchStrikes(ServerLevel level, int enchantmentLevel, Vec3 targetPos, float scalar) {
        if (scalar <= 0.0001F) return;

        float calculatedCount = this.count.calculate(enchantmentLevel) * scalar;
        int totalStrikes = (int) calculatedCount;

        if (level.random.nextFloat() < (calculatedCount - totalStrikes)) {
            totalStrikes++;
        }

        if (totalStrikes <= 0) return;

        int baseDelay = (int) this.delayTicks.calculate(enchantmentLevel);
        int variance = (int) this.delayVariance.calculate(enchantmentLevel);

        for (int i = 0; i < totalStrikes; i++) {
            int timeOffset = baseDelay + (variance > 0 ? level.random.nextInt(variance * 2) - variance : 0);
            timeOffset = Math.max(0, timeOffset);

            Runnable strikeAction = () -> {
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                if (lightning != null) {
                    lightning.snapTo(targetPos);
                    lightning.setVisualOnly(this.visualOnly);
                    level.addFreshEntity(lightning);
                }
            };

            SpellFieldTaskScheduler.schedule(level, timeOffset, strikeAction);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() { return CODEC; }
}