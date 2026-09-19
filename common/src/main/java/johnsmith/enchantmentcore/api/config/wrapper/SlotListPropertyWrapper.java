package johnsmith.enchantmentcore.api.config.wrapper;

import java.util.List;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigDescription;

import net.minecraft.world.entity.EquipmentSlotGroup;

/**
 * Wrapper for resolving a configuration property containing equipment slot groups.
 *
 * @param property    The configuration property containing the equipment slot groups.
 * @param description The description of the configuration property.
 * @param fallback    The default list of equipment slot groups to use if the property is unconfigured or empty.
 */
public record SlotListPropertyWrapper(Property<List<EquipmentSlotGroup>> property, ConfigDescription description, List<EquipmentSlotGroup> fallback) {

    /**
     * Determines if the target property is present.
     *
     * @return True if the property is not null, false otherwise.
     */
    public boolean isConfigured() {
        return this.property != null;
    }

    /**
     * Returns the configured list of equipment slot groups. Returns the fallback list if the configuration is empty.
     *
     * @param vanillaFallback The fallback equipment slot group list.
     * @return The resolved equipment slot group list.
     */
    public List<EquipmentSlotGroup> resolve(List<EquipmentSlotGroup> vanillaFallback) {
        if (!this.isConfigured()) return this.fallback != null ? this.fallback : vanillaFallback;
        List<EquipmentSlotGroup> entries = this.property.get();
        return entries == null || entries.isEmpty() ? vanillaFallback : entries;
    }
}