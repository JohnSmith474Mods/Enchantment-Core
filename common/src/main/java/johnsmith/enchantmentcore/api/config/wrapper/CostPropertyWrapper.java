package johnsmith.enchantmentcore.api.config.wrapper;

import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Composite configuration wrapper for dynamically evaluating an {@link Enchantment.Cost} structure
 * ({@code min_cost} or {@code max_cost}), encapsulating independent {@link IntPropertyWrapper} instances
 * for the {@code base} cost and the {@code per_level_above_first} increment.
 *
 * @param baseWrapper     The integer property wrapper governing the base enchanting cost, or {@code null}.
 * @param perLevelWrapper The integer property wrapper governing the cost increment per level above the first, or {@code null}.
 */
public record CostPropertyWrapper(IntPropertyWrapper baseWrapper, IntPropertyWrapper perLevelWrapper) {
    /**
     * Determines whether either the base cost wrapper or the per-level cost wrapper holds an active configuration.
     *
     * @return {@code true} if at least one sub-wrapper is non-null and configured, {@code false} otherwise.
     */
    public boolean isConfigured() {
        return (this.baseWrapper != null && this.baseWrapper.isConfigured()) ||
                (this.perLevelWrapper != null && this.perLevelWrapper.isConfigured());
    }

    /**
     * Checks whether the currently configured cost parameters differ from the provided baseline {@link Enchantment.Cost}.
     *
     * @param original The original baseline {@link Enchantment.Cost} from the enchantment definition.
     * @return {@code true} if the resolved base or per-level cost differs from {@code original}, {@code false} otherwise.
     */
    public boolean isDifferent(Enchantment.Cost original) {
        if (!this.isConfigured()) return false;
        int currentBase = this.baseWrapper != null ? this.baseWrapper.resolve(original.base()) : original.base();
        int currentPerLevel = this.perLevelWrapper != null ? this.perLevelWrapper.resolve(original.perLevelAboveFirst()) : original.perLevelAboveFirst();
        return currentBase != original.base() || currentPerLevel != original.perLevelAboveFirst();
    }

    /**
     * Constructs an updated {@link Enchantment.Cost} using the active configuration values, or returns
     * {@code original} if neither sub-property is configured.
     *
     * @param original The original baseline {@link Enchantment.Cost} used for fallback values.
     * @return A new {@link Enchantment.Cost} reflecting the active configuration, or {@code original} if unconfigured.
     */
    public Enchantment.Cost resolve(Enchantment.Cost original) {
        if (!this.isConfigured()) return original;
        int currentBase = this.baseWrapper != null ? this.baseWrapper.resolve(original.base()) : original.base();
        int currentPerLevel = this.perLevelWrapper != null ? this.perLevelWrapper.resolve(original.perLevelAboveFirst()) : original.perLevelAboveFirst();
        return new Enchantment.Cost(currentBase, currentPerLevel);
    }
}