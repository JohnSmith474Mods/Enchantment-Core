package johnsmith.enchantmentcore.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.type.PotionPayload;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public record ApplyPotionEffect(
        List<PotionPayload> potionEffects,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {

    public static final String POTION_EFFECTS = "potion_effects";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "apply_potion";

    public static final LevelBasedKeyProvider KEY_PROVIDER = PotionPayload.KEY_PROVIDER;

    public static final MapCodec<ApplyPotionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PotionPayload.CODEC.listOf().fieldOf(POTION_EFFECTS).forGetter(ApplyPotionEffect::potionEffects),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(ApplyPotionEffect::entityFilter)
    ).apply(instance, ApplyPotionEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (!(victim instanceof LivingEntity living)) return;
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        for (PotionPayload payload : this.potionEffects) {
            int duration = (int) payload.durationTicks().calculate(enchantmentLevel);
            int amplifier = (int) payload.amplifier().calculate(enchantmentLevel);
            living.addEffect(new MobEffectInstance(payload.effect(), duration, amplifier));
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() { return CODEC; }
}