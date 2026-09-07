package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.HealingType;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public record DamageHealingEffect(
        LevelBasedValue multiplier,
        HealingType healingType,
        Optional<LevelBasedValue> maxAbsorption
) {
    public static final String MULTIPLIER = "multiplier";
    public static final String HEALING_TYPE = "healing_type";
    public static final String MAX_ABSORPTION = "max_absorption";

    public static final String KEY = "healing_on_damage_received";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            MULTIPLIER, MAX_ABSORPTION
    );

    public static final Codec<DamageHealingEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(MULTIPLIER).forGetter(DamageHealingEffect::multiplier),
            HealingType.CODEC.fieldOf(HEALING_TYPE).forGetter(DamageHealingEffect::healingType),
            LevelBasedValue.CODEC.optionalFieldOf(MAX_ABSORPTION).forGetter(DamageHealingEffect::maxAbsorption)
    ).apply(instance, DamageHealingEffect::new));

    public void apply(LivingEntity living, int enchantmentLevel, float damageDealt) {
        if (!living.isAlive()) return;

        float calculatedAmount = damageDealt * this.multiplier.calculate(enchantmentLevel);
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
}