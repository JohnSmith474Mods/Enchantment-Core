package johnsmith.enchantmentcore.mixin.client.tooltip;

import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes package-private fields of ItemEnchantments for tooltip interception.
 */
@Mixin(ItemEnchantments.class)
public interface ItemEnchantmentsAccessor {

    /**
     * @return The value of the package-private 'showInTooltip' boolean.
     */
    @Accessor("showInTooltip")
    boolean enchantment_core$showInTooltip();
}