package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.living;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin targeting movement drag calculation in {@link LivingEntity#travel}.
 * Modifies terminal friction and fluid resistance dynamically through enchantment data.
 */
@Mixin(LivingEntity.class)
public abstract class DragMixin {

    /**
     * Internal calculation logic iterating through the equipment slots to extract and apply Drag multipliers.
     *
     * @param originalDrag The initial drag scalar.
     * @return The modified drag scalar.
     */
    @Unique
    private double enchantment_core$calculateDragMultiplier(double originalDrag) {
        LivingEntity entity = (LivingEntity) (Object) this;
        boolean isClient = entity.level().isClientSide();
        ServerLevel serverLevel = isClient ? null : (ServerLevel) entity.level();

        float currentDrag = (float) originalDrag;
        LootContext lootContext = null;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (enchantments.isEmpty()) continue;

            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                if (!entry.getKey().value().matchingSlot(slot)) continue;
                List<ConditionalEffect<EnchantmentValueEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.ENTITY_DRAG.get());

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
                        if (isClient || cond.matches(lootContext)) {
                            currentDrag = cond.effect().process(entry.getIntValue(), entity.getRandom(), currentDrag);
                        }
                    }
                }
            }
        }

        return currentDrag;
    }

    /**
     * Intercepts lava fluid physics execution.
     * Requires an explicit method descriptor to resolve the private method across ModDevGradle mapping states.
     */
    @WrapOperation(
            method = "travelInFluid(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"),
            require = 0
    )
    private Vec3 enchantment_core$applyEntityDragScale(Vec3 instance, double scale, Operation<Vec3> original) {
        return original.call(instance, enchantment_core$calculateDragMultiplier(scale));
    }

    /**
     * Intercepts water fluid physics execution.
     * Captures the discrete X, Y, and Z coefficients and scales them independently.
     */
    @WrapOperation(
            method = "travelInFluid(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"),
            require = 0
    )
    private Vec3 enchantment_core$applyEntityDragMultiply(Vec3 instance, double x, double y, double z, Operation<Vec3> original) {
        double newX = enchantment_core$calculateDragMultiplier(x);
        double newY = enchantment_core$calculateDragMultiplier(y);
        double newZ = enchantment_core$calculateDragMultiplier(z);
        return original.call(instance, newX, newY, newZ);
    }
}