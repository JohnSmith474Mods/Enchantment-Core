package johnsmith.enchantmentcore.client;

import johnsmith.configoverhauled.api.client.gui.factory.WidgetFactory;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreenFactory;
import johnsmith.configoverhauled.impl.client.gui.registry.DefaultWidgetRegistry;

import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.config.property.EnchantableItemListProperty;
import johnsmith.enchantmentcore.config.property.SlotListProperty;
import johnsmith.enchantmentcore.client.gui.entry.EnchantableListEntry;
import johnsmith.enchantmentcore.client.gui.entry.SlotListEntry;

import net.minecraft.client.gui.screens.Screen;

public class CommonClient {

    public static void initialize() {
        final DefaultWidgetRegistry registry = new DefaultWidgetRegistry();

        registry.register(EnchantableItemListProperty.class, (WidgetFactory) EnchantableListEntry::new);
        registry.register(SlotListProperty.class, (WidgetFactory) SlotListEntry::new);

        Config.MANAGER.setScreenFactory(parent ->
                 ConfigScreenFactory.createWithRegistry((Screen) parent, Config.MANAGER, registry)
        );
    }
}
