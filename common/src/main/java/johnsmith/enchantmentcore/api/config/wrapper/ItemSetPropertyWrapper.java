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
 * Binds a dynamic configuration property containing {@link ItemOrItems} entries to an enchantment's
 * {@code supported_items} or {@code primary_items} definition, resolving them into a {@link HolderSet} at runtime.
 *
 * @param property      The live configuration {@link Property} holding the list of items and item tags.
 * @param description   The {@link ConfigDescription} metadata identifying the configuration property.
 * @param fallbackItems The fallback list of {@link ItemOrItems} parsed from the datapack JSON definition.
 */
public record ItemSetPropertyWrapper(
        Property<List<ItemOrItems>> property,
        ConfigDescription description,
        List<ItemOrItems> fallbackItems
) {
    /**
     * Determines whether a valid configuration property is bound to this wrapper.
     *
     * @return {@code true} if {@link #property()} is non-null, {@code false} otherwise.
     */
    public boolean isConfigured() {
        return this.property != null;
    }

    /**
     * Compiles the configured {@link ItemOrItems} entries (expanding any item tags against
     * {@link BuiltInRegistries#ITEM}) into a direct {@link HolderSet}.
     * <p>
     * If the property is unconfigured or its resolved entry list is empty, {@code vanillaFallback} is returned.
     *
     * @param vanillaFallback The baseline {@link HolderSet} to return if no configured entries are available.
     * @return A {@link HolderSet} containing all resolved item holders, or {@code vanillaFallback} if empty.
     */
    public HolderSet<Item> resolve(HolderSet<Item> vanillaFallback) {
        List<ItemOrItems> entries = this.isConfigured() ? this.property.get() : this.fallbackItems;
        if (entries == null || entries.isEmpty()) return vanillaFallback;

        List<Holder<Item>> holders = new ArrayList<>();
        for (ItemOrItems entry : entries) {
            if (entry.isTag()) {
                Optional<HolderSet.Named<Item>> tagSet = BuiltInRegistries.ITEM.getTag(entry.getTag());
                tagSet.ifPresent(named -> named.forEach(holders::add));
            } else {
                Item item = entry.getItem();
                if (item != null) {
                    BuiltInRegistries.ITEM.getHolder(BuiltInRegistries.ITEM.getKey(item)).ifPresent(holders::add);
                }
            }
        }
        return HolderSet.direct(holders);
    }
}