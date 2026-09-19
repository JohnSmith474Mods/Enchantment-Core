package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.living;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Mixin targeting movement drag calculation in {@link LivingEntity#travel}.
 * Modifies terminal friction and fluid resistance dynamically through enchantment data.
 */
@Mixin(LivingEntity.class)
public abstract class DragMixin {

    /**
     * Modifies the drag coefficient applied to the movement vector during travel updates.
     *
     * @param originalDrag The vanilla velocity retention factor (0.0 to 1.0).
     * @return The modified drag factor.
     */
    @ModifyArg(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"), index = 0)
    private double enchantment_core$applyEntityDrag(double originalDrag) {
        LivingEntity entity = (LivingEntity) (Object) this;
        boolean isClient = entity.level().isClientSide();
        ServerLevel serverLevel = isClient ? null : (ServerLevel) entity.level();

        float currentDrag = (float) originalDrag;
        LootContext lootContext = null;

        // Iterate all equipment slots to collect compounding drag modifiers.
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (enchantments.isEmpty()) continue;

            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                if (!entry.getKey().value().matchingSlot(slot)) continue;
                List<ConditionalEffect<EnchantmentValueEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.ENTITY_DRAG);

                if (effects != null) {
                    if (!isClient && lootContext == null) {
                        LootParams params = new LootParams.Builder(serverLevel)
                                .withParameter(LootContextParams.THIS_ENTITY, entity)
                                .withParameter(LootContextParams.ORIGIN, entity.position())
                                .withParameter(LootContextParams.ENCHANTMENT_LEVEL, entry.getIntValue())
                                .create(LootContextParamSets.ENCHANTED_ENTITY);
                        lootContext = new LootContext.Builder(params).create(Optional.empty());
                    }

                    for (ConditionalEffect<EnchantmentValueEffect> cond : effects) {
                        // Allow clients to calculate drag without full loot predicates for prediction smoothness.
                        if (isClient || cond.matches(lootContext)) {
                            currentDrag = cond.effect().process(entry.getIntValue(), entity.getRandom(), currentDrag);
                        }
                    }
                }
            }
        }

        return currentDrag;
    }
}