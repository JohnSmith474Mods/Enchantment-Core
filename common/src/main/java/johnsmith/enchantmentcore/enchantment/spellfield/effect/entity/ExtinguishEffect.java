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

public record ExtinguishEffect(
        LevelBasedValue tickReduction,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {

    public static final String TICK_REDUCTION = "tick_reduction";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "extinguish";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(TICK_REDUCTION);

    public static final MapCodec<ExtinguishEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(TICK_REDUCTION).forGetter(ExtinguishEffect::tickReduction),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(ExtinguishEffect::entityFilter)
    ).apply(instance, ExtinguishEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        int reduction = (int) (this.tickReduction.calculate(enchantmentLevel) * volumeScalar);
        if (reduction <= 0) return;

        int currentFireTicks = victim.getRemainingFireTicks();
        if (currentFireTicks > 0) {
            victim.setRemainingFireTicks(Math.max(0, currentFireTicks - reduction));
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }
}