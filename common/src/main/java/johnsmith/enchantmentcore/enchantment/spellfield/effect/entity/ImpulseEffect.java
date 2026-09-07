package johnsmith.enchantmentcore.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.DirectionalSpellFieldEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldAxis;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record ImpulseEffect(
        LevelBasedValue strength,
        Optional<FieldAxis> direction,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect, DirectionalSpellFieldEffect {

    public static final String STRENGTH = "strength";
    public static final String DIRECTION = "direction";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "impulse";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(STRENGTH);

    public static final MapCodec<ImpulseEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(STRENGTH).forGetter(ImpulseEffect::strength),
            FieldAxis.CODEC.optionalFieldOf(DIRECTION).forGetter(ImpulseEffect::direction),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(ImpulseEffect::entityFilter)
    ).apply(instance, ImpulseEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        float force = this.strength.calculate(enchantmentLevel) * volumeScalar;
        if (Math.abs(force) < 0.0001F) return;

        FieldAxis activeDirection = this.direction.orElse(FieldAxis.RADIAL);
        Vec3 impulseDir = activeDirection.getImpulseVector(spatialReference, epicenter, victim.position());
        Vec3 impulse = impulseDir.scale(force);

        double frictionBump = (victim.onGround() && activeDirection != FieldAxis.VERTICAL && activeDirection != FieldAxis.RADIAL)
                ? Math.min(0.2D, Math.abs(force))
                : 0.0D;

        victim.setDeltaMovement(victim.getDeltaMovement().add(impulse.x, impulse.y + frictionBump, impulse.z));
        victim.hurtMarked = true;
        victim.hasImpulse = true;
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() { return CODEC; }

    @Override
    public Optional<FieldAxis> getActiveAxis() {
        return Optional.of(this.direction.orElse(FieldAxis.RADIAL));
    }
}