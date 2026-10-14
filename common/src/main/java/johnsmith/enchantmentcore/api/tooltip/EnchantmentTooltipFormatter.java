package johnsmith.enchantmentcore.api.tooltip;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Pluggable strategy contract for formatting enchantment lines and descriptions on item tooltips.
 * <p>
 * Implementations are registered in {@link TooltipFormatterRegistry} and evaluated in descending
 * order of {@link #getPriority()} whenever an item's applied or stored enchantments are rendered.
 */
public interface EnchantmentTooltipFormatter {
    /**
     * Formats the tooltip lines for a single enchantment entry on the given {@link ItemStack}.
     *
     * @param enchantment The registry {@link Holder} of the enchantment being rendered.
     * @param level       The active level of the enchantment on the item.
     * @param itemStack   The {@link ItemStack} hosting the enchantment.
     * @return An {@link Optional} containing the list of formatted {@link Component} lines to append to the tooltip,
     *         or {@link Optional#empty()} to pass evaluation to the next lower-priority formatter.
     */
    Optional<List<Component>> format(Holder<Enchantment> enchantment, int level, ItemStack itemStack);

    /**
     * Returns the execution priority of this formatter.
     * Formatters with higher priority values are queried first by {@link TooltipFormatterRegistry}.
     *
     * @return The integer priority value.
     */
    int getPriority();

    /**
     * Determines whether this formatter is currently active and eligible to format tooltips.
     *
     * @return {@code true} if enabled, {@code false} to skip this formatter.
     */
    default boolean isEnabled() {
        return true;
    }
}