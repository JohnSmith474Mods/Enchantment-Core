package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record DropItemEffect(
        LevelBasedValue chance,
        List<EquipmentSlot> slots,
        boolean pickOneRandomly
) implements EnchantmentEntityEffect {

    public static final String CHANCE = "chance";
    public static final String SLOTS = "slots";
    public static final String PICK_ONE_RANDOMLY = "pick_one_randomly";

    public static final String KEY = "drop_item";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(CHANCE);

    public static final MapCodec<DropItemEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(CHANCE).forGetter(DropItemEffect::chance),
            EquipmentSlot.CODEC.listOf().fieldOf(SLOTS).forGetter(DropItemEffect::slots),
            Codec.BOOL.optionalFieldOf(PICK_ONE_RANDOMLY, false).forGetter(DropItemEffect::pickOneRandomly)
    ).apply(instance, DropItemEffect::new));

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
}