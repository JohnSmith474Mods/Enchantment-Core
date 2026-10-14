package johnsmith.enchantmentcore.api.enchantment.effect;

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

/**
 * Data-driven enchantment effect component that restores health or grants absorption hearts to an
 * attacker proportional to the amount of combat damage dealt (life-steal / leech mechanic).
 *
 * @param multiplier    The {@link LevelBasedValue} multiplied by the inflicted damage to compute the restored amount.
 * @param healingType   The restoration mode ({@link HealingType#HEALING} or {@link HealingType#ABSORPTION}).
 * @param maxAbsorption An optional {@link LevelBasedValue} capping the maximum absorption hearts granted when using {@link HealingType#ABSORPTION}.
 */
public record DamageHealingEffect(
        LevelBasedValue multiplier,
        HealingType healingType,
        Optional<LevelBasedValue> maxAbsorption
) {
    /** JSON field key for the damage-to-healing multiplier. */
    public static final String MULTIPLIER = "multiplier";

    /** JSON field key for the healing restoration mode. */
    public static final String HEALING_TYPE = "healing_type";

    /** JSON field key for the optional maximum absorption cap. */
    public static final String MAX_ABSORPTION = "max_absorption";

    /** Registry path identifier for this effect component. */
    public static final String KEY = "healing_on_damage_received";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            MULTIPLIER, MAX_ABSORPTION
    );

    /**
     * Codec for serializing and deserializing {@link DamageHealingEffect} instances from JSON.
     */
    public static final Codec<DamageHealingEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(MULTIPLIER).forGetter(DamageHealingEffect::multiplier),
            HealingType.CODEC.fieldOf(HEALING_TYPE).forGetter(DamageHealingEffect::healingType),
            LevelBasedValue.CODEC.optionalFieldOf(MAX_ABSORPTION).forGetter(DamageHealingEffect::maxAbsorption)
    ).apply(instance, DamageHealingEffect::new));

    /**
     * Applies the proportional healing or absorption to the specified living entity based on the damage dealt.
     *
     * @param living           The entity receiving the health or absorption restoration (typically the attacker).
     * @param enchantmentLevel The active level of the enchantment.
     * @param damageDealt      The raw amount of damage inflicted that triggered this effect.
     */
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

    // region Convenience Constructors

    /**
     * Constructs a {@link DamageHealingEffect} with a defined maximum absorption cap.
     *
     * @param multiplier    The damage-to-healing multiplier.
     * @param healingType   The restoration mode.
     * @param maxAbsorption The maximum absorption heart cap.
     */
    public DamageHealingEffect(LevelBasedValue multiplier, HealingType healingType, LevelBasedValue maxAbsorption) {
        this(multiplier, healingType, Optional.of(maxAbsorption));
    }

    /**
     * Constructs a {@link DamageHealingEffect} without an absorption cap.
     *
     * @param multiplier  The damage-to-healing multiplier.
     * @param healingType The restoration mode.
     */
    public DamageHealingEffect(LevelBasedValue multiplier, HealingType healingType) {
        this(multiplier, healingType, Optional.empty());
    }
    // endregion
}