package johnsmith.enchantmentcore.api.tooltip;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Registry for enchantment tooltip formatters.
 */
public class TooltipFormatterRegistry {
    private static final List<EnchantmentTooltipFormatter> FORMATTERS = new ArrayList<>();
    private static boolean isDirty = false;

    /**
     * Registers a tooltip formatter.
     *
     * @param formatter The formatter instance to register.
     */
    public static void register(EnchantmentTooltipFormatter formatter) {
        FORMATTERS.add(formatter);
        isDirty = true;
    }

    /**
     * Retrieves the formatted tooltip for the specified enchantment and level.
     *
     * @param enchantment The target enchantment holder.
     * @param level       The level of the enchantment.
     * @return A list of formatted text components.
     */
    public static List<Component> getFormatted(Holder<Enchantment> enchantment, int level) {
        if (isDirty) {
            FORMATTERS.sort(Comparator.comparingInt(EnchantmentTooltipFormatter::getPriority).reversed());
            isDirty = false;
        }

        for (EnchantmentTooltipFormatter formatter : FORMATTERS) {
            if (!formatter.isEnabled()) {
                continue;
            }

            var result = formatter.format(enchantment, level);
            if (result.isPresent()) {
                return result.get();
            }
        }

        return List.of(Enchantment.getFullname(enchantment, level));
    }
}