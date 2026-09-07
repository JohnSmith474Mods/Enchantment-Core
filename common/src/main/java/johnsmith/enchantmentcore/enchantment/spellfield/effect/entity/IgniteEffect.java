package johnsmith.enchantmentcore.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record IgniteEffect(
        LevelBasedValue durationSeconds,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {

    public static final String DURATION_SECONDS = "duration_seconds";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "ignite";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(DURATION_SECONDS);

    public static final MapCodec<IgniteEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(DURATION_SECONDS).forGetter(IgniteEffect::durationSeconds),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(IgniteEffect::entityFilter)
    ).apply(instance, IgniteEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        float finalDuration = this.durationSeconds.calculate(enchantmentLevel) * volumeScalar;
        if (finalDuration > 0.0F) {
            victim.igniteForSeconds(finalDuration);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }
}