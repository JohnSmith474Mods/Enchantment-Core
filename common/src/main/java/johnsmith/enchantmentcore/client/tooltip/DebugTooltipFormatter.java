package johnsmith.enchantmentcore.client.tooltip;

import johnsmith.enchantmentcore.api.tooltip.EnchantmentTooltipFormatter;
import johnsmith.enchantmentcore.config.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A diagnostic formatter used to test the capabilities of the TooltipFormatterRegistry.
 * Demonstrates prepending, appending, and mutating the vanilla target text,
 * alongside utilizing the provided ItemStack context.
 */
public class DebugTooltipFormatter implements EnchantmentTooltipFormatter {

    @Override
    public Optional<List<Component>> format(Holder<Enchantment> enchantment, int level, ItemStack itemStack) {
        List<Component> tooltipLines = new ArrayList<>();

        // 1. PREPEND: Put text BEFORE the target
        tooltipLines.add(Component.literal("[Test Pre-Text] Analyzing Enchantment:")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));

        // 2. MUTATE: Alter the target text itself
        // Safely wrap the vanilla component to manipulate its style without throwing casting errors
        MutableComponent vanillaName = Component.empty().append(Enchantment.getFullname(enchantment, level));
        MutableComponent mutatedTarget = Component.literal("  >>> ")
                .withStyle(ChatFormatting.GREEN)
                .append(vanillaName.withStyle(ChatFormatting.UNDERLINE))
                .append(" <<<");
        tooltipLines.add(mutatedTarget);

        // 3. APPEND: Put text AFTER the target (Using the newly exposed ItemStack context)
        String itemName = itemStack.isEmpty() ? "Unknown Item" : itemStack.getHoverName().getString();
        tooltipLines.add(Component.literal("  [Test Post-Text] Hosted on: " + itemName)
                .withStyle(ChatFormatting.DARK_AQUA));

        // Add a blank line for padding between multiple enchantments
        tooltipLines.add(Component.empty());

        return Optional.of(tooltipLines);
    }

    @Override
    public int getPriority() {
        // Use a very high priority to guarantee it overrides any other formatters during testing
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isEnabled() {
        // Can be tied to a debug config flag, but forced to true for immediate testing
        return Config.ENABLE_DEBUG_TOOLTIP_FORMATTER.get();
    }
}