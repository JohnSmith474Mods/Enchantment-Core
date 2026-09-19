package johnsmith.enchantmentcore.api.tooltip;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Optional;

/**
 * Contract for formatting enchantment tooltips.
 */
public interface EnchantmentTooltipFormatter {
    /**
     * Formats the tooltip for the specified enchantment and level.
     *
     * @param enchantment The target enchantment holder.
     * @param level       The level of the enchantment.
     * @return An optional list of formatted text components.
     */
    Optional<List<Component>> format(Holder<Enchantment> enchantment, int level);

    /**
     * Retrieves the execution priority of this formatter.
     * Higher values execute earlier.
     *
     * @return The priority integer.
     */
    int getPriority();

    /**
     * Determines if this formatter is active.
     *
     * @return True if the formatter is active, false otherwise.
     */
    default boolean isEnabled() {
        return true;
    }
}