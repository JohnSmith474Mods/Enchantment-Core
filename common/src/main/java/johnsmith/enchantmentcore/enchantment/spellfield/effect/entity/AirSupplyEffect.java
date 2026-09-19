package johnsmith.enchantmentcore.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record AirSupplyEffect(
        LevelBasedValue amount,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {

    public static final String AMOUNT = "amount";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "air_supply";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(AMOUNT);

    public static final MapCodec<AirSupplyEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(AMOUNT).forGetter(AirSupplyEffect::amount),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(AirSupplyEffect::entityFilter)
    ).apply(instance, AirSupplyEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        int change = (int) (this.amount.calculate(enchantmentLevel) * volumeScalar);
        if (change == 0) return;

        int newAir = Mth.clamp(victim.getAirSupply() + change, -20, victim.getMaxAirSupply());
        victim.setAirSupply(newAir);
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }
}