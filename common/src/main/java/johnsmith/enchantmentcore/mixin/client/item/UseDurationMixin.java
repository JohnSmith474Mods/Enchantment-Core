package johnsmith.enchantmentcore.mixin.client.item;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.Map;

import johnsmith.enchantmentcore.enchantment.effect.BowChargeTimeEffect;
import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.UseDuration;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
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
     * @param stack    The item stack being evaluated.
     * @param level    The client level context.
     * @param entity   The entity holding and using the item.
     * @param seed     The random seed.
     * @return The scaled use time.
     */
    @ModifyReturnValue(
            method = "get",
            at = @At("RETURN")
    )
    private float enchantment_core$overrideBowPullProperty(float original, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
        // Ensure the entity exists, the item is a bow, and it is actively being drawn (original > 0)
        if (entity != null && stack.is(Items.BOW) && original > 0.0F) {
            float baseTime = 20.0F;
            float reductionSeconds = 0.0F;

            ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

            // Aggregate charge time reductions from all active enchantments on the item.
            for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
                BowChargeTimeEffect effect = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.BOW_CHARGE_TIME.get());
                if (effect != null) {
                    reductionSeconds += effect.amount().calculate(entry.getValue());
                }
            }

            // Calculate the modified maximum charge threshold required to reach full power.
            float newMaxCharge = baseTime + (reductionSeconds * 20.0F);

            // Clamp to prevent division by zero or negative charge times.
            newMaxCharge = Math.max(0.1F, newMaxCharge);

            // Scale the returned use time up so that the vanilla JSON's 0.05 scale multiplier
            // resolves to 1.0 (full draw) exactly at the new dynamic max charge.
            return original * (baseTime / newMaxCharge);
        }

        return original;
    }
}