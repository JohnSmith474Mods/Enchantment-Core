package johnsmith.enchantmentcore.config.property;

import java.util.ArrayList;
import java.util.Optional;

import com.mojang.serialization.Codec;

import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.impl.core.state.DefaultProperty;

import johnsmith.enchantmentcore.api.config.data.ItemOrItems;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class EnchantableItemListProperty extends DefaultProperty.List<ItemOrItems> {
    public EnchantableItemListProperty(java.lang.String name, Group parent, ConfigScope scope, java.lang.String description, java.util.List<ItemOrItems> defaultValue, Codec<ItemOrItems> elementCodec, boolean requiresRestart) {
        super(name, parent, scope, description, defaultValue, elementCodec, requiresRestart);
    }

    public static java.util.List<net.minecraft.world.item.Item> expandToItems(java.util.List<java.lang.String> entries) {
        java.util.List<net.minecraft.world.item.Item> items = new ArrayList<>();
        for (java.lang.String entry : entries) {
            if (entry.startsWith("#")) {
                ResourceLocation tagLoc = ResourceLocation.tryParse(entry.substring(1));
                if (tagLoc != null) {
                    TagKey<net.minecraft.world.item.Item> tagKey = TagKey.create(Registries.ITEM, tagLoc);
                    Optional<HolderSet.Named<net.minecraft.world.item.Item>> tagSet = BuiltInRegistries.ITEM.get(tagKey);
                    tagSet.ifPresent(named -> named.forEach(holder -> items.add(holder.value())));
                }
            } else {
                ResourceLocation itemLoc = ResourceLocation.tryParse(entry);
                if (itemLoc != null && BuiltInRegistries.ITEM.containsKey(itemLoc)) {
                    items.add(BuiltInRegistries.ITEM.get(itemLoc).get().value());
                }
            }
        }
        return items;
    }

    public static java.util.List<java.lang.String> collapseToKeys(java.util.List<net.minecraft.world.item.Item> items) {
        java.util.List<java.lang.String> keys = new ArrayList<>();
        for (net.minecraft.world.item.Item item : items) {
            ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
            if (loc != null) {
                keys.add(loc.toString());
            }
        }
        return keys;
    }

    public static boolean isItemEnchantable(net.minecraft.world.item.Item item) {
        boolean hasEnchantableTag = item.builtInRegistryHolder().tags()
                .anyMatch(tag -> tag.location().getPath().contains("enchantable"));

        if (hasEnchantableTag) return true;
        if (item == net.minecraft.world.item.Items.BOOK) return true;

        return item.components().has(DataComponents.MAX_DAMAGE) &&
                item.components().getOrDefault(DataComponents.MAX_STACK_SIZE, 1) == 1;
    }

    public HolderSet<net.minecraft.world.item.Item> resolveToHolderSet(HolderSet<net.minecraft.world.item.Item> fallback) {
        java.util.List<ItemOrItems> entries = this.get();
        if (entries == null || entries.isEmpty()) return fallback;

        java.util.List<Holder<net.minecraft.world.item.Item>> holders = new ArrayList<>();

        for (ItemOrItems entry : entries) {
            if (entry.isTag()) {
                Optional<HolderSet.Named<net.minecraft.world.item.Item>> tagSet = BuiltInRegistries.ITEM.get(entry.getTag());
                tagSet.ifPresent(named -> named.forEach(holders::add));
            } else {
                BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(entry.getItem())).ifPresent(holders::add);
            }
        }

        return HolderSet.direct(holders);
    }
}