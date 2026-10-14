package johnsmith.enchantmentcore.client;

import johnsmith.enchantmentcore.api.client.gui.EnchantmentCoreWidgets;
import johnsmith.enchantmentcore.api.tooltip.TooltipFormatterRegistry;
import johnsmith.enchantmentcore.client.tooltip.DebugTooltipFormatter;
import johnsmith.enchantmentcore.config.Config;

public class CommonClient {
    public static void initialize() {
        Config.MANAGER.setScreenFactory(EnchantmentCoreWidgets.createScreenFactory(Config.MANAGER));
        TooltipFormatterRegistry.registerFormatter(new DebugTooltipFormatter());
    }
}