package johnsmith.enchantmentcore.api.config.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.enchantmentcore.api.config.data.ItemOrItems;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * Wrapper for resolving a configuration property containing item lists into a holder set.
 *
 * @param property      The configuration property containing the item list.
 * @param description   The description of the configuration property.
 * @param fallbackItems The default list of items to use if the property is unconfigured or empty.
 */
public record ItemSetPropertyWrapper(Property<List<ItemOrItems>> property, ConfigDescription description, List<ItemOrItems> fallbackItems) {

    /**
     * Determines if the target property is present.
     *
     * @return True if the property is not null, false otherwise.
     */
    public boolean isConfigured() {
        return this.property != null;
    }

    /**
     * Compiles the configured items and tags into a direct holder set. Returns the fallback set if the configuration is empty.
     *
     * @param vanillaFallback The fallback holder set.
     * @return The resolved holder set.
     */
    public HolderSet<Item> resolve(HolderSet<Item> vanillaFallback) {
        List<ItemOrItems> entries = this.isConfigured() ? this.property.get() : this.fallbackItems;
        if (entries == null || entries.isEmpty()) return vanillaFallback;

        List<Holder<Item>> holders = new ArrayList<>();
        for (ItemOrItems entry : entries) {
            if (entry.isTag()) {
                Optional<HolderSet.Named<Item>> tagSet = BuiltInRegistries.ITEM.get(entry.getTag());
                tagSet.ifPresent(named -> named.forEach(holders::add));
            } else {
                Item item = entry.getItem();
                if (item != null) {
                    BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).ifPresent(holders::add);
                }
            }
        }
        return HolderSet.direct(holders);
    }
}