package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

/**
 * An {@link EnchantmentEntityEffect} that forces a target {@link LivingEntity} to unequip and drop
 * items from specified {@link EquipmentSlot}s into the world with a 40-tick pickup delay.
 * <p>
 * Armor pieces enchanted with Curse of Binding ({@link EnchantmentEffectComponents#PREVENT_ARMOR_CHANGE})
 * are automatically excluded from being disarmed.
 *
 * @param chance          The {@link LevelBasedValue} governing the probability (0.0 to 1.0) that the disarm triggers.
 * @param slots           The list of candidate {@link EquipmentSlot}s eligible to be dropped.
 * @param pickOneRandomly If {@code true}, only one randomly chosen populated slot from {@code slots} is dropped;
 *                        if {@code false}, all populated candidate slots are dropped simultaneously.
 */
public record DropItemEffect(
        LevelBasedValue chance,
        List<EquipmentSlot> slots,
        boolean pickOneRandomly
) implements EnchantmentEntityEffect {
    /** JSON field key for the drop trigger probability. */
    public static final String CHANCE = "chance";

    /** JSON field key for the list of targeted equipment slots. */
    public static final String SLOTS = "slots";

    /** JSON field key for selecting a single random slot versus dropping all matching slots. */
    public static final String PICK_ONE_RANDOMLY = "pick_one_randomly";

    /** Registry path identifier for this entity effect. */
    public static final String KEY = "drop_item";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(CHANCE);

    /**
     * MapCodec for serializing and deserializing {@link DropItemEffect} instances from JSON.
     */
    public static final MapCodec<DropItemEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(CHANCE).forGetter(DropItemEffect::chance),
            EquipmentSlot.CODEC.listOf().fieldOf(SLOTS).forGetter(DropItemEffect::slots),
            Codec.BOOL.optionalFieldOf(PICK_ONE_RANDOMLY, false).forGetter(DropItemEffect::pickOneRandomly)
    ).apply(instance, DropItemEffect::new));

    /**
     * Checks whether the given item stack has any enchantment with the {@link EnchantmentEffectComponents#PREVENT_ARMOR_CHANGE}
     * component (such as Curse of Binding).
     *
     * @param stack The item stack to inspect.
     * @return {@code true} if armor removal is prevented, {@code false} otherwise.
     */
    private static boolean hasBindingCurse(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return false;

        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.value().effects().has(EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Evaluates the drop probability and forces the target living entity to drop items from the configured
     * equipment slots.
     *
     * @param level            The server level in which the effect is triggered.
     * @param enchantmentLevel The level of the source enchantment.
     * @param itemInUse        The context of the enchanted item in use.
     * @param target           The entity being disarmed.
     * @param origin           The origin position vector of the trigger.
     */
    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse itemInUse, Entity target, Vec3 origin) {
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return;

        float dropChance = this.chance.calculate(enchantmentLevel);
        if (living.getRandom().nextFloat() >= dropChance) return;

        List<EquipmentSlot> candidateSlots = new ArrayList<>();
        for (EquipmentSlot slot : this.slots) {
            ItemStack stack = living.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                if (slot.isArmor() && hasBindingCurse(stack)) {
                    continue;
                }
                candidateSlots.add(slot);
            }
        }

        if (candidateSlots.isEmpty()) return;

        List<EquipmentSlot> slotsToDrop = new ArrayList<>();
        if (this.pickOneRandomly) {
            slotsToDrop.add(candidateSlots.get(living.getRandom().nextInt(candidateSlots.size())));
        } else {
            slotsToDrop.addAll(candidateSlots);
        }

        for (EquipmentSlot slot : slotsToDrop) {
            ItemStack stackToDrop = living.getItemBySlot(slot).copy();
            living.setItemSlot(slot, ItemStack.EMPTY);
            ItemEntity dropped = living.spawnAtLocation(level, stackToDrop);
            if (dropped != null) {
                dropped.setPickUpDelay(40);
            }
        }
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }

    // region Wither Pattern

    /**
     * Constructs a {@link DropItemEffect} with an initially empty slot list for fluent builder-style configuration.
     *
     * @param chance          The probability of dropping equipment.
     * @param pickOneRandomly Whether to pick only one slot at random from the configured slots.
     */
    public DropItemEffect(LevelBasedValue chance, boolean pickOneRandomly) {
        this(chance, List.of(), pickOneRandomly);
    }

    /**
     * Returns a copy of this {@link DropItemEffect} with the specified equipment slots appended to the target list.
     *
     * @param slot One or more {@link EquipmentSlot}s to append.
     * @return A new {@link DropItemEffect} instance containing the combined slot list.
     */
    public DropItemEffect addSlot(EquipmentSlot... slot) {
        List<EquipmentSlot> updated = new ArrayList<>(this.slots);
        updated.addAll(List.of(slot));
        return new DropItemEffect(this.chance, List.copyOf(updated), this.pickOneRandomly);
    }
    // endregion
}