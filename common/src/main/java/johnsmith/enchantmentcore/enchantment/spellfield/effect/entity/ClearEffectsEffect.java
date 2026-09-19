package johnsmith.enchantmentcore.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record ClearEffectsEffect(
        LevelBasedValue durationReduction,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {

    public static final String DURATION_REDUCTION = "duration_reduction";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "clear_effects";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(DURATION_REDUCTION);

    public static final MapCodec<ClearEffectsEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(DURATION_REDUCTION).forGetter(ClearEffectsEffect::durationReduction),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(ClearEffectsEffect::entityFilter)
    ).apply(instance, ClearEffectsEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (!(victim instanceof LivingEntity living)) return;
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        int reduction = (int) (this.durationReduction.calculate(enchantmentLevel) * volumeScalar);
        if (reduction <= 0) return;

        List<MobEffectInstance> toReplace = new ArrayList<>();
        List<Holder<MobEffect>> toRemove = new ArrayList<>();

        for (MobEffectInstance instance : living.getActiveEffects()) {
            int newDuration = instance.getDuration() - reduction;

            if (newDuration <= 0) {
                toRemove.add(instance.getEffect());
            } else {
                MobEffectInstance modifiedInstance = new MobEffectInstance(
                        instance.getEffect(),
                        newDuration,
                        instance.getAmplifier(),
                        instance.isAmbient(),
                        instance.isVisible(),
                        instance.showIcon()
                );
                toReplace.add(modifiedInstance);
            }
        }

        for (Holder<MobEffect> effect : toRemove) {
            living.removeEffect(effect);
        }

        for (MobEffectInstance modifiedInstance : toReplace) {
            living.addEffect(modifiedInstance);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }
}