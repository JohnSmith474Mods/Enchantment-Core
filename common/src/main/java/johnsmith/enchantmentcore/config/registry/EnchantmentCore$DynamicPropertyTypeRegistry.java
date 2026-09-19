package johnsmith.enchantmentcore.config.registry;

import java.util.List;

import com.mojang.serialization.Codec;

import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry.TypeDefinition;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.config.property.EnchantableItemListProperty;
import johnsmith.enchantmentcore.api.config.data.ItemOrItems;
import johnsmith.enchantmentcore.config.property.SlotListProperty;

import net.minecraft.world.entity.EquipmentSlotGroup;

public class EnchantmentCore$DynamicPropertyTypeRegistry {
    public static final TypeDefinition<List<ItemOrItems>> ENCHANTABLE_ITEM_LIST = DynamicPropertyTypeRegistry.register(
            Constants.MOD_ID, "enchantable_item_list", Codec.list(ItemOrItems.CODEC),
            (name, group, def, min, max, codec) -> new EnchantableItemListProperty(name, group, ConfigScope.LEVEL, null, (List<ItemOrItems>) def, ItemOrItems.CODEC, true)
    );

    public static final TypeDefinition<List<EquipmentSlotGroup>> SLOT_LIST = DynamicPropertyTypeRegistry.register(
            Constants.MOD_ID, "slot_list", EquipmentSlotGroup.CODEC.listOf(),
            (name, group, def, min, max, codec) -> new SlotListProperty(name, group, ConfigScope.LEVEL, null, (List<EquipmentSlotGroup>) def, EquipmentSlotGroup.CODEC, true)
    );
}
