package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.type.PotionPayload;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldEntityEffect} that spawns a shrinking {@link AreaEffectCloud} populated with
 * configured {@link PotionPayload}s at the position of each targeted entity within a spell field volume.
 *
 * @param potionEffects The list of {@link PotionPayload} status effects added to the spawned area effect cloud.
 * @param cloudRadius   The {@link LevelBasedValue} initial radius (in blocks) of the spawned cloud.
 * @param entityFilter  Optional {@link ContextAwarePredicate} restricting which entities trigger cloud spawning.
 */
public record SummonLingeringPotionEffect(
        List<PotionPayload> potionEffects,
        LevelBasedValue cloudRadius,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    /** JSON field key for the list of potion payloads. */
    public static final String POTION_EFFECTS = "potion_effects";

    /** JSON field key for the initial cloud radius. */
    public static final String CLOUD_RADIUS = "cloud_radius";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "summon_lingering_potion";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(CLOUD_RADIUS);

    /**
     * MapCodec for serializing and deserializing {@link SummonLingeringPotionEffect} instances from JSON.
     */
    public static final MapCodec<SummonLingeringPotionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PotionPayload.CODEC.listOf().fieldOf(POTION_EFFECTS).forGetter(SummonLingeringPotionEffect::potionEffects),
            LevelBasedValue.CODEC.fieldOf(CLOUD_RADIUS).forGetter(SummonLingeringPotionEffect::cloudRadius),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(SummonLingeringPotionEffect::entityFilter)
    ).apply(instance, SummonLingeringPotionEffect::new));

    /**
     * Spawns an {@link AreaEffectCloud} at {@code victim}'s position with a 10-tick wait time,
     * linear radius decay over its duration, and all configured {@link #potionEffects()}.
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

        AreaEffectCloud cloud = new AreaEffectCloud(level, victim.getX(), victim.getY(), victim.getZ());
        cloud.setRadius(this.cloudRadius.calculate(enchantmentLevel));
        cloud.setWaitTime(10);
        cloud.setRadiusPerTick(-cloud.getRadius() / (float) cloud.getDuration());

        if (context.owner() instanceof LivingEntity owner) {
            cloud.setOwner(owner);
        }

        for (PotionPayload payload : this.potionEffects) {
            int duration = (int) payload.durationTicks().calculate(enchantmentLevel);
            int amplifier = (int) payload.amplifier().calculate(enchantmentLevel);
            cloud.addEffect(new MobEffectInstance(payload.effect(), duration, amplifier));
        }

        level.addFreshEntity(cloud);
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Wither Pattern

    /**
     * Constructs an unfiltered {@link SummonLingeringPotionEffect} with an initially empty payload list.
     *
     * @param cloudRadius The {@link LevelBasedValue} initial cloud radius.
     */
    public SummonLingeringPotionEffect(LevelBasedValue cloudRadius) {
        this(List.of(), cloudRadius, Optional.empty());
    }

    /**
     * Constructs an unfiltered {@link SummonLingeringPotionEffect} with the specified initial radius and payloads.
     *
     * @param cloudRadius   The {@link LevelBasedValue} initial cloud radius.
     * @param potionPayload One or more {@link PotionPayload}s to include in the cloud.
     */
    public SummonLingeringPotionEffect(LevelBasedValue cloudRadius, PotionPayload... potionPayload) {
        this(List.of(potionPayload), cloudRadius, Optional.empty());
    }

    /**
     * Returns a copy of this effect with the specified {@link PotionPayload}s appended.
     *
     * @param potionPayload One or more {@link PotionPayload}s to append.
     * @return A new {@link SummonLingeringPotionEffect} instance.
     */
    public SummonLingeringPotionEffect addPotionEffect(PotionPayload... potionPayload) {
        List<PotionPayload> updated = new ArrayList<>(this.potionEffects);
        updated.addAll(List.of(potionPayload));
        return new SummonLingeringPotionEffect(List.copyOf(updated), this.cloudRadius, this.entityFilter);
    }

    /**
     * Returns a copy of this effect restricted by the specified entity predicate filter.
     *
     * @param entityFilter The predicate filter to match against candidate entities.
     * @return A new {@link SummonLingeringPotionEffect} instance.
     */
    public SummonLingeringPotionEffect withEntityFilter(ContextAwarePredicate entityFilter) {
        return new SummonLingeringPotionEffect(potionEffects, cloudRadius, Optional.of(entityFilter));
    }
    // endregion
}