package johnsmith.enchantmentcore.client.gui.entry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.entry.OptionEntry;
import johnsmith.enchantmentcore.client.gui.screen.SlotListSelectionScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;

import java.util.ArrayList;
import java.util.List;

public class SlotListEntry extends OptionEntry<List<EquipmentSlotGroup>, Button> {
    public SlotListEntry(Property<List<EquipmentSlotGroup>> property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged);
        this.updateWidgetValue();
    }

    @Override
    protected Button createWidget() {
        return Button.builder(Component.empty(), button -> this.openSelectionScreen())
                .bounds(0, 0, 75, 20)
                .build();
    }

    private void openSelectionScreen() {
        List<EquipmentSlotGroup> initialSelection = new ArrayList<>(this.property.get());

        this.minecraft.setScreen(new SlotListSelectionScreen(
                (Screen) this.parentScreen,
                Component.translatable(this.property.translationKey()),
                initialSelection,
                selected -> {
                    // OptionEntry natively handles network sync, saving to disk, and the reset button
                    this.setValue(new ArrayList<>(selected));
                    this.updateWidgetValue();
                }
        ));
    }

    @Override
    protected void updateWidgetValue() {
        if (this.widget != null) {
            this.widget.setMessage(Component.literal(this.property.get().size() + " Slots"));
        }
    }

    @Override
    protected Component getDefaultValueTooltip() {
        return Component.literal("Default: " + this.property.defaultValue().size() + " Slots");
    }
}