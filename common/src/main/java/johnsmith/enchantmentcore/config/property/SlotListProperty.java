package johnsmith.enchantmentcore.config.property;

import com.mojang.serialization.Codec;

import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.impl.core.state.DefaultProperty;

import net.minecraft.world.entity.EquipmentSlotGroup;

public class SlotListProperty extends DefaultProperty.List<EquipmentSlotGroup> {
    public SlotListProperty(java.lang.String name, Group parent, ConfigScope scope, java.lang.String description, java.util.List<EquipmentSlotGroup> defaultValue, Codec<EquipmentSlotGroup> elementCodec, boolean requiresRestart) {
        super(name, parent, scope, description, defaultValue, elementCodec, requiresRestart);
    }
}