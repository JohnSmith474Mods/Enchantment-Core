package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

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

/**
 * An {@link EnchantmentEntityEffect} that restores health points or grants temporary absorption hearts
 * to a target {@link LivingEntity}.
 *
 * @param amount        The {@link LevelBasedValue} governing the flat amount of health or absorption restored.
 * @param healingType   The restoration mode ({@link HealingType#HEALING} or {@link HealingType#ABSORPTION}).
 * @param maxAbsorption An optional {@link LevelBasedValue} capping the maximum absorption hearts when using {@link HealingType#ABSORPTION}.
 */
public record HealingEntityEffect(
        LevelBasedValue amount,
        HealingType healingType,
        Optional<LevelBasedValue> maxAbsorption
) implements EnchantmentEntityEffect {
    /** JSON field key for the restored health or absorption amount. */
    public static final String AMOUNT = "amount";

    /** JSON field key for the healing restoration mode. */
    public static final String HEALING_TYPE = "healing_type";

    /** JSON field key for the optional maximum absorption cap. */
    public static final String MAX_ABSORPTION = "max_absorption";

    /** Registry path identifier for this entity effect. */
    public static final String KEY = "healing";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(AMOUNT, MAX_ABSORPTION);

    /**
     * MapCodec for serializing and deserializing {@link HealingEntityEffect} instances from JSON.
     */
    public static final MapCodec<HealingEntityEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(AMOUNT).forGetter(HealingEntityEffect::amount),
            HealingType.CODEC.fieldOf(HEALING_TYPE).forGetter(HealingEntityEffect::healingType),
            LevelBasedValue.CODEC.optionalFieldOf(MAX_ABSORPTION).forGetter(HealingEntityEffect::maxAbsorption)
    ).apply(instance, HealingEntityEffect::new));

    /**
     * Applies the calculated healing or absorption to the target entity if it is a living, alive entity.
     *
     * @param level            The server level in which the effect is triggered.
     * @param enchantmentLevel The level of the source enchantment.
     * @param itemInUse        The context of the enchanted item in use.
     * @param target           The entity receiving the healing effect.
     * @param origin           The origin position vector of the trigger.
     */
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

    // region Convenience Constructors

    /**
     * Constructs a {@link HealingEntityEffect} with an explicit absorption cap.
     *
     * @param amount        The amount of health or absorption to restore.
     * @param healingType   The restoration mode.
     * @param maxAbsorption The maximum absorption cap.
     */
    public HealingEntityEffect(LevelBasedValue amount, HealingType healingType, LevelBasedValue maxAbsorption) {
        this(amount, healingType, Optional.of(maxAbsorption));
    }

    /**
     * Constructs a {@link HealingEntityEffect} without an absorption cap.
     *
     * @param amount      The amount of health or absorption to restore.
     * @param healingType The restoration mode.
     */
    public HealingEntityEffect(LevelBasedValue amount, HealingType healingType) {
        this(amount, healingType, Optional.empty());
    }
    // endregion
}