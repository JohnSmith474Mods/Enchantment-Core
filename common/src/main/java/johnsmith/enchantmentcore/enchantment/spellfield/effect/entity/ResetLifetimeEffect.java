package johnsmith.enchantmentcore.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.mixin.enchantment.accessor.ExperienceOrbAccessor;
import johnsmith.enchantmentcore.mixin.enchantment.accessor.ItemEntityAccessor;

import java.util.Optional;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public record ResetLifetimeEffect(
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    public static final String KEY = "reset_lifetime";

    public static final MapCodec<ResetLifetimeEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ContextAwarePredicate.CODEC.optionalFieldOf("entity_filter").forGetter(ResetLifetimeEffect::entityFilter)
    ).apply(instance, ResetLifetimeEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        // Reset the despawn timer to 0 for floating entities
        if (victim instanceof ItemEntity item) {
            ((ItemEntityAccessor) item).enchantment_core$setAge(0);
        } else if (victim instanceof ExperienceOrb orb) {
            ((ExperienceOrbAccessor) orb).enchantment_core$setAge(0);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }
}