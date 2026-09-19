package johnsmith.enchantmentcore.mixin.client.item;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Map;

import johnsmith.enchantmentcore.enchantment.effect.BowChargeTimeEffect;
import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;

import net.minecraft.client.renderer.item.properties.numeric.UseDuration;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin targeting the UseDuration numeric property.
 * Synchronizes the visual draw speed of the bow with active charge time enchantments.
 */
@Mixin(UseDuration.class)
public class UseDurationMixin {

    /**
     * Intercepts the evaluation of the UseDuration numeric property.
     * Artificially scales the returned tick duration for bows to accelerate the visual drawing animation.
     *
     * @param original The original time the item has been used in ticks.
     * @param stack    The item stack being evaluated resolved via Local capture.
     * @param owner    The entity owner holding and using the item resolved via Local capture.
     * @return The scaled use time.
     */
    @ModifyReturnValue(
            method = "get(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/ItemOwner;I)F",
            at = @At("RETURN")
    )
    private float enchantment_core$overrideBowPullProperty(
            float original,
            @Local(argsOnly = true) ItemStack stack,
            @Local(argsOnly = true) ItemOwner owner
    ) {
        LivingEntity entity = owner == null ? null : owner.asLivingEntity();

        if (entity != null && stack.is(Items.BOW) && original > 0.0F) {
            float baseTime = 20.0F;
            float reductionSeconds = 0.0F;

            ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

            for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
                BowChargeTimeEffect effect = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.BOW_CHARGE_TIME.get());
                if (effect != null) {
                    reductionSeconds += effect.amount().calculate(entry.getValue());
                }
            }

            float newMaxCharge = baseTime + (reductionSeconds * 20.0F);
            newMaxCharge = Math.max(0.1F, newMaxCharge);

            return original * (baseTime / newMaxCharge);
        }

        return original;
    }
}