package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.type.PotionPayload;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldEntityEffect} that applies one or more configured {@link PotionPayload} status
 * effects directly to targeted {@link LivingEntity} instances within a spell field volume.
 *
 * @param potionEffects The list of {@link PotionPayload} definitions to apply.
 * @param entityFilter  Optional {@link ContextAwarePredicate} restricting which entities receive the effects.
 */
public record ApplyPotionEffect(
        List<PotionPayload> potionEffects,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    /** JSON field key for the list of potion payloads. */
    public static final String POTION_EFFECTS = "potion_effects";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "apply_potion";

    /**
     * Supplies the JSON property keys holding {@link net.minecraft.world.item.enchantment.LevelBasedValue}
     * definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = PotionPayload.KEY_PROVIDER;

    /**
     * MapCodec for serializing and deserializing {@link ApplyPotionEffect} instances from JSON.
     */
    public static final MapCodec<ApplyPotionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PotionPayload.CODEC.listOf().fieldOf(POTION_EFFECTS).forGetter(ApplyPotionEffect::potionEffects),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(ApplyPotionEffect::entityFilter)
    ).apply(instance, ApplyPotionEffect::new));

    /**
     * Evaluates and applies each configured {@link PotionPayload} as a {@link MobEffectInstance}
     * to {@code victim} if it is a {@link LivingEntity} and matches {@link #entityFilter()}.
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
        if (!(victim instanceof LivingEntity living)) return;
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        for (PotionPayload payload : this.potionEffects) {
            int duration = (int) payload.durationTicks().calculate(enchantmentLevel);
            int amplifier = (int) payload.amplifier().calculate(enchantmentLevel);
            living.addEffect(new MobEffectInstance(payload.effect(), duration, amplifier));
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() { return CODEC; }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link ApplyPotionEffect} with the specified potion payloads.
     *
     * @param potionPayload One or more {@link PotionPayload}s to apply.
     */
    public ApplyPotionEffect(PotionPayload... potionPayload) {
        this(List.of(potionPayload), Optional.empty());
    }

    /**
     * Constructs an {@link ApplyPotionEffect} with a single potion payload and an entity filter.
     *
     * @param potionPayload The {@link PotionPayload} to apply.
     * @param entityFilter  The predicate filter to match against candidate entities.
     */
    public ApplyPotionEffect(PotionPayload potionPayload, ContextAwarePredicate entityFilter) {
        this(List.of(potionPayload), Optional.of(entityFilter));
    }

    /**
     * Constructs an {@link ApplyPotionEffect} with an entity filter and an initially empty payload list.
     *
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public ApplyPotionEffect(ContextAwarePredicate entityFilter) {
        this(List.of(), Optional.of(entityFilter));
    }
    // endregion

    // region Wither Pattern

    /**
     * Constructs an empty, unfiltered {@link ApplyPotionEffect} for fluent configuration.
     */
    public ApplyPotionEffect() {
        this(List.of(), Optional.empty());
    }

    /**
     * Returns a copy of this effect with the specified {@link PotionPayload}s appended.
     *
     * @param potionPayload One or more {@link PotionPayload}s to append.
     * @return A new {@link ApplyPotionEffect} instance containing the combined payload list.
     */
    public ApplyPotionEffect addPotionEffect(PotionPayload... potionPayload) {
        List<PotionPayload> updated = new ArrayList<>(this.potionEffects);
        updated.addAll(List.of(potionPayload));
        return new ApplyPotionEffect(List.copyOf(updated), entityFilter);
    }
    // endregion
}