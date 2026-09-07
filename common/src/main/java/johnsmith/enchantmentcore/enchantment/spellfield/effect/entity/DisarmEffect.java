package johnsmith.enchantmentcore.enchantment.spellfield.effect.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
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

public record DisarmEffect(
        LevelBasedValue dropChance,
        List<EquipmentSlot> slots,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {

    public static final String DROP_CHANCE = "drop_chance";
    public static final String SLOTS = "slots";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "disarm";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(DROP_CHANCE);

    private static final Codec<EquipmentSlot> SLOT_CODEC = Codec.STRING.xmap(EquipmentSlot::byName, EquipmentSlot::getName);

    public static final MapCodec<DisarmEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(DROP_CHANCE).forGetter(DisarmEffect::dropChance),
            SLOT_CODEC.listOf().fieldOf(SLOTS).forGetter(DisarmEffect::slots),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(DisarmEffect::entityFilter)
    ).apply(instance, DisarmEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (!(victim instanceof LivingEntity living)) return;
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        float chance = this.dropChance.calculate(enchantmentLevel) * volumeScalar;
        if (chance <= 0.0F) return;

        for (EquipmentSlot slot : this.slots) {
            ItemStack equipped = living.getItemBySlot(slot);

            if (!equipped.isEmpty()) {
                if (chance >= 1.0F || level.random.nextFloat() <= chance) {
                    ItemEntity droppedItem = new ItemEntity(level, living.getX(), living.getY() + 1.0D, living.getZ(), equipped.copy());
                    droppedItem.setPickUpDelay(40);

                    float f = level.random.nextFloat() * 0.5F;
                    float f1 = level.random.nextFloat() * ((float) Math.PI * 2.0F);
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
}