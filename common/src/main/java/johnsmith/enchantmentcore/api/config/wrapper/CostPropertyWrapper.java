package johnsmith.enchantmentcore.api.config.wrapper;

import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Wrapper for dynamic enchantment cost calculation.
 *
 * @param baseWrapper     The configuration property wrapper for the base cost.
 * @param perLevelWrapper The configuration property wrapper for the per-level cost multiplier.
 */
public record CostPropertyWrapper(IntPropertyWrapper baseWrapper, IntPropertyWrapper perLevelWrapper) {

    /**
     * Determines if base or per-level properties contain active configurations.
     *
     * @return True if at least one property wrapper is configured, false otherwise.
     */
    public boolean isConfigured() {
        return (this.baseWrapper != null && this.baseWrapper.isConfigured()) ||
                (this.perLevelWrapper != null && this.perLevelWrapper.isConfigured());
    }

    /**
     * Compares the configured cost values against the original cost values.
     *
     * @param original The original enchantment cost.
     * @return True if the calculated values differ from the original values, false otherwise.
     */
    public boolean isDifferent(Enchantment.Cost original) {
        if (!this.isConfigured()) return false;

        int currentBase = this.baseWrapper != null ? this.baseWrapper.resolve(original.base()) : original.base();
        int currentPerLevel = this.perLevelWrapper != null ? this.perLevelWrapper.resolve(original.perLevelAboveFirst()) : original.perLevelAboveFirst();

        return currentBase != original.base() || currentPerLevel != original.perLevelAboveFirst();
    }

    /**
     * Calculates the new enchantment cost using the configured properties or returns the original cost.
     *
     * @param original The original enchantment cost.
     * @return The resolved enchantment cost.
     */
    public Enchantment.Cost resolve(Enchantment.Cost original) {
        if (!this.isConfigured()) return original;

        int currentBase = this.baseWrapper != null ? this.baseWrapper.resolve(original.base()) : original.base();
        int currentPerLevel = this.perLevelWrapper != null ? this.perLevelWrapper.resolve(original.perLevelAboveFirst()) : original.perLevelAboveFirst();

        return new Enchantment.Cost(currentBase, currentPerLevel);
    }
}