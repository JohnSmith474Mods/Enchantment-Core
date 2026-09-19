package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.living.player;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;

import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting the player inventory to evaluate and execute continuous enchantment effects.
 * Scans equipped slots during the server tick cycle and dispatches the INVENTORY_TICK component.
 */
@Mixin(Inventory.class)
public abstract class InventoryTickMixin {

    @Shadow @Final public Player player;
    @Shadow @Final public NonNullList<ItemStack> items;
    @Shadow @Final public NonNullList<ItemStack> armor;
    @Shadow @Final public NonNullList<ItemStack> offhand;
    @Shadow public int selected;

    /**
     * Intercepts the conclusion of the inventory tick sequence.
     * Maps physical inventory arrays to logical equipment slots and initiates item evaluation.
     *
     * @param ci The callback information.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void enchantment_core$onInventoryTick(CallbackInfo ci) {
        if (this.player.level().isClientSide()) return;

        LootParams.Builder paramsBuilder = null;

        // Map armor array indices to specific armor equipment slots.
        for (int i = 0; i < this.armor.size(); i++) {
            EquipmentSlot slot = switch (i) {
                case 0 -> EquipmentSlot.FEET;
                case 1 -> EquipmentSlot.LEGS;
                case 2 -> EquipmentSlot.CHEST;
                case 3 -> EquipmentSlot.HEAD;
                default -> null;
            };
            paramsBuilder = enchantment_core$processItem(this.armor.get(i), slot, paramsBuilder);
        }

        // Map offhand array to the offhand equipment slot.
        for (int i = 0; i < this.offhand.size(); i++) {
            paramsBuilder = enchantment_core$processItem(this.offhand.get(i), EquipmentSlot.OFFHAND, paramsBuilder);
        }

        // Map main inventory array. Assign MAINHAND strictly to the actively selected hotbar index.
        // Assign null to all unequipped background items.
        for (int i = 0; i < this.items.size(); i++) {
            EquipmentSlot slot = (i == this.selected) ? EquipmentSlot.MAINHAND : null;
            paramsBuilder = enchantment_core$processItem(this.items.get(i), slot, paramsBuilder);
        }
    }

    /**
     * Evaluates a single item stack for valid inventory tick enchantment components.
     * Enforces slot configuration constraints and executes matched effects.
     *
     * @param item          The item stack to evaluate.
     * @param slot          The active equipment slot containing the item. Null if unequipped.
     * @param paramsBuilder The persistent loot parameter builder state.
     * @return The updated loot parameter builder state.
     */
    @Unique
    private LootParams.Builder enchantment_core$processItem(ItemStack item, EquipmentSlot slot, LootParams.Builder paramsBuilder) {
        if (item.isEmpty()) return paramsBuilder;

        ItemEnchantments enchantments = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return paramsBuilder;

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {

            // Require the item to reside in a configured operational slot.
            // Bypasses unequipped backpack items.
            if (slot == null || !entry.getKey().value().matchingSlot(slot)) {
                continue;
            }

            List<ConditionalEffect<EnchantmentEntityEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.INVENTORY_TICK.get());

            if (effects != null && !effects.isEmpty()) {
                // Instantiate the parameter builder lazily to eliminate overhead on items lacking the effect.
                if (paramsBuilder == null) {
                    paramsBuilder = new LootParams.Builder((ServerLevel) this.player.level())
                            .withParameter(LootContextParams.THIS_ENTITY, this.player)
                            .withParameter(LootContextParams.ORIGIN, this.player.position());
                }

                LootParams params = paramsBuilder.withParameter(LootContextParams.ENCHANTMENT_LEVEL, entry.getIntValue()).create(LootContextParamSets.ENCHANTED_ENTITY);
                LootContext lootContext = new LootContext.Builder(params).create(Optional.empty());

                EnchantedItemInUse inUse = new EnchantedItemInUse(item, slot, this.player);

                for (ConditionalEffect<EnchantmentEntityEffect> conditionalEffect : effects) {
                    if (conditionalEffect.matches(lootContext)) {
                        conditionalEffect.effect().apply((ServerLevel) this.player.level(), entry.getIntValue(), inUse, this.player, this.player.position());
                    }
                }
            }
        }

        return paramsBuilder;
    }
}