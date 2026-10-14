package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

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

/**
 * A {@link SpellFieldEntityEffect} that inflicts damage of a specified {@link DamageType} to targeted
 * entities within a spell field volume, scaled by each target's local topological multiplier.
 *
 * @param damageType   The registry {@link ResourceKey} of the {@link DamageType} to inflict.
 * @param amount       The {@link LevelBasedValue} base damage amount before topological scaling.
 * @param entityFilter Optional {@link ContextAwarePredicate} restricting which entities take damage.
 */
public record DamageEffect(
        ResourceKey<DamageType> damageType,
        LevelBasedValue amount,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    /** JSON field key for the damage type resource key. */
    public static final String DAMAGE_TYPE = "damage_type";

    /** JSON field key for the base damage amount. */
    public static final String AMOUNT = "amount";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "damage_entity";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(AMOUNT);

    /**
     * MapCodec for serializing and deserializing {@link DamageEffect} instances from JSON.
     */
    public static final MapCodec<DamageEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.DAMAGE_TYPE).fieldOf(DAMAGE_TYPE).forGetter(DamageEffect::damageType),
            LevelBasedValue.CODEC.fieldOf(AMOUNT).forGetter(DamageEffect::amount),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(DamageEffect::entityFilter)
    ).apply(instance, DamageEffect::new));

    /**
     * Inflicts $\text{amount} \cdot \text{volumeScalar}$ damage of type {@link #damageType()} to {@code victim},
     * attributing {@code spatialReference} as the direct entity and {@code context.owner()} as the causing entity.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param victim           The target entity intersecting the spell field.
     * @param volumeScalar     The active topological multiplier for {@code victim}.
     */
    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        float finalDamage = this.amount.calculate(enchantmentLevel) * volumeScalar;
        if (finalDamage <= 0.0F) return;

        net.minecraft.core.Holder<DamageType> damageHolder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(this.damageType);
        DamageSource source = new DamageSource(damageHolder, spatialReference, context.owner());

        victim.hurt(source, finalDamage);
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link DamageEffect}.
     *
     * @param damageType The {@link DamageType} resource key.
     * @param amount     The {@link LevelBasedValue} base damage amount.
     */
    public DamageEffect(ResourceKey<DamageType> damageType, LevelBasedValue amount) {
        this(damageType, amount, Optional.empty());
    }

    /**
     * Constructs a {@link DamageEffect} restricted by the given entity predicate filter.
     *
     * @param damageType   The {@link DamageType} resource key.
     * @param amount       The {@link LevelBasedValue} base damage amount.
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public DamageEffect(ResourceKey<DamageType> damageType, LevelBasedValue amount, ContextAwarePredicate entityFilter) {
        this(damageType, amount, Optional.of(entityFilter));
    }
    // endregion
}