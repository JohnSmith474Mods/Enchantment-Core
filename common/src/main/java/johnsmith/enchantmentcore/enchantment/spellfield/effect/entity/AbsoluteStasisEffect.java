package johnsmith.enchantmentcore.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public record AbsoluteStasisEffect(
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    public static final String KEY = "absolute_stasis";

    public static final MapCodec<AbsoluteStasisEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ContextAwarePredicate.CODEC.optionalFieldOf("entity_filter").forGetter(AbsoluteStasisEffect::entityFilter)
    ).apply(instance, AbsoluteStasisEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        // 1. Clamp all momentum and gravity accumulation
        victim.setDeltaMovement(Vec3.ZERO);

        // 2. Prevent lethal fall damage from accumulating while suspended
        victim.fallDistance = 0.0F;

        // 3. Force the server to synchronize the frozen state to the client
        victim.hasImpulse = true;
        victim.hurtMarked = true;
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }
}