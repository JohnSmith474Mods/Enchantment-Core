package johnsmith.enchantmentcore.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record DamageEffect(
        ResourceKey<DamageType> damageType,
        LevelBasedValue amount,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {

    public static final String DAMAGE_TYPE = "damage_type";
    public static final String AMOUNT = "amount";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "damage_entity";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(AMOUNT);

    public static final MapCodec<DamageEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.DAMAGE_TYPE).fieldOf(DAMAGE_TYPE).forGetter(DamageEffect::damageType),
            LevelBasedValue.CODEC.fieldOf(AMOUNT).forGetter(DamageEffect::amount),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(DamageEffect::entityFilter)
    ).apply(instance, DamageEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        float finalDamage = this.amount.calculate(enchantmentLevel) * volumeScalar;
        if (finalDamage <= 0.0F) return;

        net.minecraft.core.Holder<DamageType> damageHolder = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(this.damageType);
        DamageSource source = new DamageSource(damageHolder, spatialReference, context.owner());
        victim.hurt(source, finalDamage);
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }
}