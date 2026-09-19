package johnsmith.enchantmentcore.client.gui.entry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.entry.AbstractRegistryEntry;
import johnsmith.configoverhauled.impl.client.gui.entry.registry.SafeIconHelper;

import johnsmith.enchantmentcore.api.config.data.ItemOrItems;
import johnsmith.enchantmentcore.client.gui.screen.EnchantableListSelectionScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public class EnchantableListEntry extends AbstractRegistryEntry<Item, List<ItemOrItems>> {
    public EnchantableListEntry(Property<List<ItemOrItems>> property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(
                property,
                parentScreen,
                minecraft,
                onValueChanged,
                BuiltInRegistries.ITEM,
                item -> {
                    Item boundItem = BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).get().value();
                    return SafeIconHelper.getSafeIcon(boundItem);
                },
                item -> Component.translatable(item.getDescriptionId())
        );
        this.updateWidgetValue();
    }

    @Override
    protected void openSelectionScreen() {
        List<ItemOrItems> initialSelection = new ArrayList<>(this.property.get());

        this.minecraft.gui.setScreen(new EnchantableListSelectionScreen(
                (Screen) this.parentScreen,
                Component.translatable(this.property.translationKey()),
                initialSelection,
                this::onScreenClosed
        ));
    }

    private void onScreenClosed(List<ItemOrItems> selected) {
        this.applySelection(new ArrayList<>(selected));
    }

    @Override
    protected void updateWidgetValue() {
        this.widget.setMessage(Component.literal(this.property.get().size() + " Elements"));
    }

    @Override
    protected Component getDefaultValueTooltip() {
        return Component.literal("Default: " + this.property.defaultValue().size() + " Elements");
    }
}