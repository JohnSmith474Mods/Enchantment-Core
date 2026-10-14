package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldEntityEffect} that probabilistically forces targeted {@link LivingEntity} instances
 * to drop items equipped in specified {@link EquipmentSlot}s into the world with a 40-tick pickup delay
 * and a randomized outward scatter impulse.
 *
 * @param dropChance   The {@link LevelBasedValue} base probability (0.0 to 1.0) of disarming each slot before topological scaling.
 * @param slots        The list of {@link EquipmentSlot}s evaluated for disarming.
 * @param entityFilter Optional {@link ContextAwarePredicate} restricting which entities can be disarmed.
 */
public record DisarmEffect(
        LevelBasedValue dropChance,
        List<EquipmentSlot> slots,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {
    /** JSON field key for the base disarm probability. */
    public static final String DROP_CHANCE = "drop_chance";

    /** JSON field key for the list of targeted equipment slots. */
    public static final String SLOTS = "slots";

    /** JSON field key for the optional entity filter predicate. */
    public static final String ENTITY_FILTER = "entity_filter";

    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "disarm";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(DROP_CHANCE);

    /** Internal string codec mapping equipment slots by their lowercase name. */
    private static final Codec<EquipmentSlot> SLOT_CODEC = Codec.STRING.xmap(EquipmentSlot::byName, EquipmentSlot::getName);

    /**
     * MapCodec for serializing and deserializing {@link DisarmEffect} instances from JSON.
     */
    public static final MapCodec<DisarmEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(DROP_CHANCE).forGetter(DisarmEffect::dropChance),
            SLOT_CODEC.listOf().fieldOf(SLOTS).forGetter(DisarmEffect::slots),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(DisarmEffect::entityFilter)
    ).apply(instance, DisarmEffect::new));

    /**
     * Evaluates the scaled disarm probability ($\text{dropChance} \cdot \text{volumeScalar}$) for each
     * populated slot in {@link #slots()} on {@code victim}, spawning an {@link ItemEntity} and clearing
     * the slot whenever the roll succeeds.
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

        float chance = this.dropChance.calculate(enchantmentLevel) * volumeScalar;
        if (chance <= 0.0F) return;

        for (EquipmentSlot slot : this.slots) {
            ItemStack equipped = living.getItemBySlot(slot);
            if (!equipped.isEmpty()) {
                if (chance >= 1.0F || level.getRandom().nextFloat() <= chance) {
                    ItemEntity droppedItem = new ItemEntity(level, living.getX(), living.getY() + 1.0D, living.getZ(), equipped.copy());
                    droppedItem.setPickUpDelay(40);

                    float f = level.getRandom().nextFloat() * 0.5F;
                    float f1 = level.getRandom().nextFloat() * ((float) Math.PI * 2.0F);
                    droppedItem.setDeltaMovement((-Mth.sin(f1) * f), 0.2D, (Mth.cos(f1) * f));

                    level.addFreshEntity(droppedItem);
                    living.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }

    // region Convenience Constructors

    /**
     * Constructs an unfiltered {@link DisarmEffect} targeting a single equipment slot.
     *
     * @param dropChance The {@link LevelBasedValue} disarm probability.
     * @param slots      The single {@link EquipmentSlot} to target.
     */
    public DisarmEffect(LevelBasedValue dropChance, EquipmentSlot slots) {
        this(dropChance, List.of(slots), Optional.empty());
    }

    /**
     * Constructs a {@link DisarmEffect} targeting a single equipment slot and restricted by an entity filter.
     *
     * @param dropChance   The {@link LevelBasedValue} disarm probability.
     * @param slots        The single {@link EquipmentSlot} to target.
     * @param entityFilter The predicate filter to match against candidate entities.
     */
    public DisarmEffect(LevelBasedValue dropChance, EquipmentSlot slots, ContextAwarePredicate entityFilter) {
        this(dropChance, List.of(slots), Optional.of(entityFilter));
    }
    // endregion

    // region Wither Pattern

    /**
     * Constructs a {@link DisarmEffect} with an initially empty slot list for fluent builder-style configuration.
     *
     * @param dropChance The {@link LevelBasedValue} disarm probability.
     */
    public DisarmEffect(LevelBasedValue dropChance) {
        this(dropChance, List.of(), Optional.empty());
    }

    /**
     * Returns a copy of this effect with the specified {@link EquipmentSlot}s appended to the target list.
     *
     * @param slot One or more {@link EquipmentSlot}s to append.
     * @return A new {@link DisarmEffect} instance containing the combined slot list.
     */
    public DisarmEffect addSlot(EquipmentSlot... slot) {
        List<EquipmentSlot> updated = new ArrayList<>(this.slots);
        updated.addAll(List.of(slot));
        return new DisarmEffect(this.dropChance, List.copyOf(updated), this.entityFilter);
    }
    // endregion
}