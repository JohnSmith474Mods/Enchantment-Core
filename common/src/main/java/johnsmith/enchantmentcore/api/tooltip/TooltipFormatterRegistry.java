package johnsmith.enchantmentcore.api.tooltip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Client-side registry and dispatch pipeline for {@link EnchantmentTooltipFormatter} implementations.
 * <p>
 * Invoked by {@code ItemStackMixin} when rendering {@code minecraft:enchantments} or
 * {@code minecraft:stored_enchantments} tooltip components. Evaluates enabled formatters in descending
 * priority order and falls back to {@link Enchantment#getFullname(Holder, int)} if no formatter handles the entry.
 */
public class TooltipFormatterRegistry {
    /** Internal list of registered tooltip formatters. */
    private static final List<EnchantmentTooltipFormatter> FORMATTERS = new ArrayList<>();

    /** Flag indicating whether {@link #FORMATTERS} requires priority sorting before the next lookup. */
    private static boolean isDirty = false;

    /**
     * Registers an {@link EnchantmentTooltipFormatter} and marks the priority order as dirty.
     *
     * @param formatter The formatter instance to register.
     */
    public static void registerFormatter(EnchantmentTooltipFormatter formatter) {
        FORMATTERS.add(formatter);
        isDirty = true;
    }

    /**
     * Evaluates registered, enabled {@link EnchantmentTooltipFormatter}s in descending priority order
     * and returns the first non-empty formatted tooltip component list for the given enchantment.
     *
     * @param enchantment The target enchantment {@link Holder}.
     * @param level       The active level of the enchantment.
     * @param itemStack   The {@link ItemStack} hosting the enchantment.
     * @return A list of formatted {@link Component} lines, or a singleton list containing {@link Enchantment#getFullname(Holder, int)} if unhandled.
     */
    public static List<Component> getFormatted(Holder<Enchantment> enchantment, int level, ItemStack itemStack) {
        if (isDirty) {
            FORMATTERS.sort(Comparator.comparingInt(EnchantmentTooltipFormatter::getPriority).reversed());
            isDirty = false;
        }

        for (EnchantmentTooltipFormatter formatter : FORMATTERS) {
            if (!formatter.isEnabled()) {
                continue;
            }
            var result = formatter.format(enchantment, level, itemStack);
            if (result.isPresent()) {
                return result.get();
            }
        }

        return List.of(Enchantment.getFullname(enchantment, level));
    }
}