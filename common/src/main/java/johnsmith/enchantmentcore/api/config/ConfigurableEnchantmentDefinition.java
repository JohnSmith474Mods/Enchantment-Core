package johnsmith.enchantmentcore.api.config;

import johnsmith.enchantmentcore.api.config.wrapper.CostPropertyWrapper;
import johnsmith.enchantmentcore.api.config.wrapper.IntPropertyWrapper;
import johnsmith.enchantmentcore.api.config.wrapper.ItemSetPropertyWrapper;
import johnsmith.enchantmentcore.api.config.wrapper.SlotListPropertyWrapper;

import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Duck interface injected into {@link Enchantment} via mixin to attach and query live configuration
 * property wrappers on top of static enchantment definitions.
 * <p>
 * Populated during deserialization by {@code EnchantmentCodecMixin} and queried at runtime by
 * {@code EnchantmentMixin} to dynamically rebuild {@link Enchantment.EnchantmentDefinition} records
 * whenever configured values deviate from their datapack fallbacks.
 */
public interface ConfigurableEnchantmentDefinition {
    /**
     * Attaches the dynamic configuration wrapper controlling the enchantment's maximum level.
     *
     * @param wrapper The {@link IntPropertyWrapper} for {@code max_level}.
     */
    void enchantment_core$setMaxLevelWrapper(IntPropertyWrapper wrapper);

    /**
     * Retrieves the dynamic configuration wrapper controlling the enchantment's maximum level.
     *
     * @return The attached {@link IntPropertyWrapper}, or {@code null} if not configured.
     */
    IntPropertyWrapper enchantment_core$getMaxLevelWrapper();

    /**
     * Attaches the dynamic configuration wrapper controlling the enchantment's rarity weight.
     *
     * @param wrapper The {@link IntPropertyWrapper} for {@code weight}.
     */
    void enchantment_core$setWeightWrapper(IntPropertyWrapper wrapper);

    /**
     * Retrieves the dynamic configuration wrapper controlling the enchantment's rarity weight.
     *
     * @return The attached {@link IntPropertyWrapper}, or {@code null} if not configured.
     */
    IntPropertyWrapper enchantment_core$getWeightWrapper();

    /**
     * Attaches the dynamic configuration wrapper controlling the enchantment's anvil combination cost.
     *
     * @param wrapper The {@link IntPropertyWrapper} for {@code anvil_cost}.
     */
    void enchantment_core$setAnvilCostWrapper(IntPropertyWrapper wrapper);

    /**
     * Retrieves the dynamic configuration wrapper controlling the enchantment's anvil combination cost.
     *
     * @return The attached {@link IntPropertyWrapper}, or {@code null} if not configured.
     */
    IntPropertyWrapper enchantment_core$getAnvilCostWrapper();

    /**
     * Attaches the dynamic configuration wrapper controlling the enchantment's minimum enchanting table cost.
     *
     * @param wrapper The {@link CostPropertyWrapper} for {@code min_cost}.
     */
    void enchantment_core$setMinCostWrapper(CostPropertyWrapper wrapper);

    /**
     * Retrieves the dynamic configuration wrapper controlling the enchantment's minimum enchanting table cost.
     *
     * @return The attached {@link CostPropertyWrapper}, or {@code null} if not configured.
     */
    CostPropertyWrapper enchantment_core$getMinCostWrapper();

    /**
     * Attaches the dynamic configuration wrapper controlling the enchantment's maximum enchanting table cost.
     *
     * @param wrapper The {@link CostPropertyWrapper} for {@code max_cost}.
     */
    void enchantment_core$setMaxCostWrapper(CostPropertyWrapper wrapper);

    /**
     * Retrieves the dynamic configuration wrapper controlling the enchantment's maximum enchanting table cost.
     *
     * @return The attached {@link CostPropertyWrapper}, or {@code null} if not configured.
     */
    CostPropertyWrapper enchantment_core$getMaxCostWrapper();

    /**
     * Attaches the dynamic configuration wrapper controlling the set of items compatible with this enchantment.
     *
     * @param wrapper The {@link ItemSetPropertyWrapper} for {@code supported_items}.
     */
    void enchantment_core$setSupportedItemsWrapper(ItemSetPropertyWrapper wrapper);

    /**
     * Retrieves the dynamic configuration wrapper controlling the set of items compatible with this enchantment.
     *
     * @return The attached {@link ItemSetPropertyWrapper}, or {@code null} if not configured.
     */
    ItemSetPropertyWrapper enchantment_core$getSupportedItemsWrapper();

    /**
     * Attaches the dynamic configuration wrapper controlling the primary items for this enchantment in the enchanting table.
     *
     * @param wrapper The {@link ItemSetPropertyWrapper} for {@code primary_items}.
     */
    void enchantment_core$setPrimaryItemsWrapper(ItemSetPropertyWrapper wrapper);

    /**
     * Retrieves the dynamic configuration wrapper controlling the primary items for this enchantment in the enchanting table.
     *
     * @return The attached {@link ItemSetPropertyWrapper}, or {@code null} if not configured.
     */
    ItemSetPropertyWrapper enchantment_core$getPrimaryItemsWrapper();

    /**
     * Attaches the dynamic configuration wrapper controlling the active equipment slot groups for this enchantment.
     *
     * @param wrapper The {@link SlotListPropertyWrapper} for {@code slots}.
     */
    void enchantment_core$setSlotsWrapper(SlotListPropertyWrapper wrapper);

    /**
     * Retrieves the dynamic configuration wrapper controlling the active equipment slot groups for this enchantment.
     *
     * @return The attached {@link SlotListPropertyWrapper}, or {@code null} if not configured.
     */
    SlotListPropertyWrapper enchantment_core$getSlotsWrapper();
}