package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldEntityEffect} that teleports targeted {@link LivingEntity} instances to a random
 * safe location within {@link #range()} blocks (similar to chorus fruit consumption), dismounting
 * passengers and playing teleportation sound effects.
 *
 * @param range        The {@link LevelBasedValue} maximum horizontal and vertical teleportation radius in blocks.
 * @param entityFilter Optional {@link ContextAwarePredicate} restricting which entities are teleported.
 */
public record RandomTeleportEffect(
        LevelBasedValue range,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    /** JSON field key for the teleportation radius in blocks. */
    public static final String RANGE = "range";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "random_teleport";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(RANGE);

    /**
     * MapCodec for serializing and deserializing {@link RandomTeleportEffect} instances from JSON.
     */
    public static final MapCodec<RandomTeleportEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(RANGE).forGetter(RandomTeleportEffect::range),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(RandomTeleportEffect::entityFilter)
    ).apply(instance, RandomTeleportEffect::new));

    /**
     * Attempts up to 16 times to teleport {@code victim} to a random valid coordinate within
     * {@link #range()} blocks via {@link LivingEntity#randomTeleport(double, double, double, boolean)}.
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

        double tpRange = this.range.calculate(enchantmentLevel);

        for (int i = 0; i < 16; i++) {
            double x = living.getX() + (living.getRandom().nextDouble() - 0.5D) * tpRange * 2.0D;
            double y = Mth.clamp(living.getY() + (living.getRandom().nextInt((int) tpRange * 2) - tpRange), level.getMinY(), level.getMaxY() - 1);
            double z = living.getZ() + (living.getRandom().nextDouble() - 0.5D) * tpRange * 2.0D;

            if (living.isPassenger()) living.stopRiding();

            Vec3 oldPos = living.position();
            if (living.randomTeleport(x, y, z, true)) {
                level.playSound(null, oldPos.x, oldPos.y, oldPos.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                living.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
                break;
            }
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link RandomTeleportEffect}.
     *
     * @param range The {@link LevelBasedValue} teleportation radius in blocks.
     */
    public RandomTeleportEffect(LevelBasedValue range) {
        this(range, Optional.empty());
    }

    /**
     * Constructs a {@link RandomTeleportEffect} restricted by the given entity predicate filter.
     *
     * @param range        The {@link LevelBasedValue} teleportation radius in blocks.
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public RandomTeleportEffect(LevelBasedValue range, ContextAwarePredicate entityFilter) {
        this(range, Optional.of(entityFilter));
    }
    // endregion
}