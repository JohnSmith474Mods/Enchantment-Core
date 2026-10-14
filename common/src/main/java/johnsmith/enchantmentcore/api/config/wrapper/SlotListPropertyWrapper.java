package johnsmith.enchantmentcore.api.config.wrapper;

import java.util.List;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigDescription;

import net.minecraft.world.entity.EquipmentSlotGroup;

/**
 * Binds a dynamic configuration property containing a list of {@link EquipmentSlotGroup} values to an
 * enchantment's {@code slots} definition.
 *
 * @param property    The live configuration {@link Property} holding the list of equipment slot groups.
 * @param description The {@link ConfigDescription} metadata identifying the configuration property.
 * @param fallback    The fallback list of {@link EquipmentSlotGroup} values parsed from the datapack JSON definition.
 */
public record SlotListPropertyWrapper(
        Property<List<EquipmentSlotGroup>> property,
        ConfigDescription description,
        List<EquipmentSlotGroup> fallback
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
     * Resolves the active list of {@link EquipmentSlotGroup} values from the bound configuration property.
     * <p>
     * Falls back to {@link #fallback()} or {@code vanillaFallback} if the property is unconfigured or empty.
     *
     * @param vanillaFallback The baseline equipment slot group list from the enchantment definition.
     * @return The resolved list of {@link EquipmentSlotGroup} instances.
     */
    public List<EquipmentSlotGroup> resolve(List<EquipmentSlotGroup> vanillaFallback) {
        if (!this.isConfigured()) return this.fallback != null ? this.fallback : vanillaFallback;
        List<EquipmentSlotGroup> entries = this.property.get();
        return entries == null || entries.isEmpty() ? vanillaFallback : entries;
    }
}