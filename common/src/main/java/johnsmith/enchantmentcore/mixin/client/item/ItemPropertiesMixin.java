package johnsmith.enchantmentcore.mixin.client.item;

import java.util.Map;

import johnsmith.enchantmentcore.enchantment.effect.BowChargeTimeEffect;
import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;

import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin targeting the item property registry.
 * Replaces the vanilla bow pulling animation property to synchronize visual draw speed with active charge time enchantments.
 */
@Mixin(ItemProperties.class)
public class ItemPropertiesMixin {

    /**
     * Intercepts the registration of item property functions.
     * Overrides the 'pull' property for the bow item.
     *
     * @param original The original vanilla property function.
     * @param item     The item being registered.
     * @param name     The identifier of the property being registered.
     * @return The replacement property function if the target is the bow pull property, otherwise the original function.
     */
    @ModifyVariable(
            method = "register(Lnet/minecraft/world/item/Item;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/renderer/item/ClampedItemPropertyFunction;)V",
            at = @At("HEAD"),
            index = 2,
            argsOnly = true
    )
    private static ClampedItemPropertyFunction overrideBowPullProperty(ClampedItemPropertyFunction original, Item item, ResourceLocation name) {
        if (item == Items.BOW && name.equals(ResourceLocation.withDefaultNamespace("pull"))) {

            return (stack, level, entity, seed) -> {
                if (entity == null) {
                    return 0.0F;
                }
                if (entity.getUseItem() != stack) {
                    return 0.0F;
                }

                // Determine the total ticks the item has been actively drawn.
                int chargeTicks = stack.getUseDuration(entity) - entity.getUseItemRemainingTicks();

                float baseTime = 20.0F;
                float reductionSeconds = 0.0F;

                ItemEnchantments enchantments = stack.getEnchantments();

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

                // Return the normalized progress ratio (0.0 to 1.0) mapping the current ticks against the dynamic max charge.
                return (float) (chargeTicks / newMaxCharge);
            };
        }

        return original;
    }
}