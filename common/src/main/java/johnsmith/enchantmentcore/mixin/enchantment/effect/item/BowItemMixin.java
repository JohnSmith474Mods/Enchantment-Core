package johnsmith.enchantmentcore.mixin.enchantment.effect.item;

import com.llamalad7.mixinextras.sugar.Local;

import java.util.Map;

import johnsmith.enchantmentcore.enchantment.effect.BowChargeTimeEffect;
import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;

import net.minecraft.core.Holder;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Mixin targeting the BowItem class to introduce dynamic draw-speed modifications.
 * Modifies the perceived charge duration during the arrow velocity calculation phase.
 */
@Mixin(BowItem.class)
public class BowItemMixin {

    /**
     * Intercepts the charge ticks argument passed to the getPowerForTime calculation.
     * Artificially inflates the charge value based on the presence of draw-speed reduction enchantments.
     * This bypasses the need to manipulate local ticking variables directly.
     *
     * @param chargeTicks The actual number of ticks the player held the bow.
     * @param stack       The bow item stack captured from the local method scope.
     * @return The modified charge ticks simulating a faster draw speed.
     */
    @ModifyArg(
            method = "releaseUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"
            ),
            index = 0
    )
    private int modifyBowChargeArgument(int chargeTicks, @Local(argsOnly = true) ItemStack stack) {
        // Establish the baseline vanilla full-charge duration (20 ticks equals 1.0 second).
        float baseTime = 20.0F;
        float reductionSeconds = 0.0F;

        ItemEnchantments enchantments = stack.getEnchantments();

        // Iterate all active enchantments to aggregate charge time reductions.
        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
            BowChargeTimeEffect effect = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.BOW_CHARGE_TIME);

            if (effect != null) {
                int lvl = entry.getValue();
                // Accumulate the configured reduction value (e.g., -0.25 seconds).
                reductionSeconds += effect.amount().calculate(lvl);
            }
        }

        // Bypass recalculation if no reductions are active.
        if (reductionSeconds == 0.0F) {
            return chargeTicks;
        }

        // Calculate the new theoretical maximum charge time required to reach full power.
        // Formula: 20 ticks + (reduction_in_seconds * 20 ticks_per_second).
        float newMaxCharge = baseTime + (reductionSeconds * 20.0F);

        // Enforce a strict minimum of 0.1 ticks to prevent division by zero errors.
        newMaxCharge = Math.max(0.1F, newMaxCharge);

        // Calculate the artificial acceleration multiplier.
        // Example: If new max charge is 10 ticks, the multiplier becomes 20 / 10 = 2.0.
        // One actual tick counts as two virtual ticks toward the power calculation.
        float multiplier = baseTime / newMaxCharge;

        return (int) (chargeTicks * multiplier);
    }
}