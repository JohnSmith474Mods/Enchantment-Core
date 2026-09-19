package johnsmith.enchantmentcore.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.type.PotionPayload;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record SummonLingeringPotionEffect(
        List<PotionPayload> potionEffects,
        LevelBasedValue cloudRadius,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {

    public static final String POTION_EFFECTS = "potion_effects";
    public static final String CLOUD_RADIUS = "cloud_radius";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "summon_lingering_potion";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(CLOUD_RADIUS);

    public static final MapCodec<SummonLingeringPotionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PotionPayload.CODEC.listOf().fieldOf(POTION_EFFECTS).forGetter(SummonLingeringPotionEffect::potionEffects),
            LevelBasedValue.CODEC.fieldOf(CLOUD_RADIUS).forGetter(SummonLingeringPotionEffect::cloudRadius),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(SummonLingeringPotionEffect::entityFilter)
    ).apply(instance, SummonLingeringPotionEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        AreaEffectCloud cloud = new AreaEffectCloud(level, victim.getX(), victim.getY(), victim.getZ());
        cloud.setRadius(this.cloudRadius.calculate(enchantmentLevel));
        cloud.setWaitTime(10);
        cloud.setRadiusPerTick(-cloud.getRadius() / (float)cloud.getDuration());

        if (context.owner() instanceof LivingEntity owner) {
            cloud.setOwner(owner);
        }

        for (PotionPayload payload : this.potionEffects) {
            int duration = (int) payload.durationTicks().calculate(enchantmentLevel);
            int amplifier = (int) payload.amplifier().calculate(enchantmentLevel);
            cloud.addEffect(new MobEffectInstance(payload.effect(), duration, amplifier));
        }

        level.addFreshEntity(cloud);
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() { return CODEC; }
}