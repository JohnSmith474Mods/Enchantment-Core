package johnsmith.enchantmentcore.enchantment.effect;

import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.enchantment.HealingType;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

public record HealingEntityEffect(
        LevelBasedValue amount,
        HealingType healingType,
        Optional<LevelBasedValue> maxAbsorption
) implements EnchantmentEntityEffect {

    public static final String AMOUNT = "amount";
    public static final String HEALING_TYPE = "healing_type";
    public static final String MAX_ABSORPTION = "max_absorption";

    public static final String KEY = "healing";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(AMOUNT, MAX_ABSORPTION);

    public static final MapCodec<HealingEntityEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(AMOUNT).forGetter(HealingEntityEffect::amount),
            HealingType.CODEC.fieldOf(HEALING_TYPE).forGetter(HealingEntityEffect::healingType),
            LevelBasedValue.CODEC.optionalFieldOf(MAX_ABSORPTION).forGetter(HealingEntityEffect::maxAbsorption)
    ).apply(instance, HealingEntityEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse itemInUse, Entity target, Vec3 origin) {
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return;

        float calculatedAmount = this.amount.calculate(enchantmentLevel);
        if (calculatedAmount <= 0.0F) return;

        if (this.healingType == HealingType.HEALING) {
            living.heal(calculatedAmount);
        } else if (this.healingType == HealingType.ABSORPTION) {
            float currentAbsorption = living.getAbsorptionAmount();
            float newAbsorption = currentAbsorption + calculatedAmount;

            if (this.maxAbsorption.isPresent()) {
                float cap = this.maxAbsorption.get().calculate(enchantmentLevel);
                if (currentAbsorption >= cap) return;
                newAbsorption = Math.min(newAbsorption, cap);
            }

            AttributeInstance maxAbsorbAttr = living.getAttribute(Attributes.MAX_ABSORPTION);
            if (maxAbsorbAttr != null && maxAbsorbAttr.getBaseValue() < newAbsorption) {
                maxAbsorbAttr.setBaseValue(newAbsorption);
            }

            living.setAbsorptionAmount(newAbsorption);
        }
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}