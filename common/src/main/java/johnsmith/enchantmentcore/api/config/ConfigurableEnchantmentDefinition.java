package johnsmith.enchantmentcore.api.config;

import johnsmith.enchantmentcore.api.config.wrapper.CostPropertyWrapper;
import johnsmith.enchantmentcore.api.config.wrapper.IntPropertyWrapper;
import johnsmith.enchantmentcore.api.config.wrapper.ItemSetPropertyWrapper;
import johnsmith.enchantmentcore.api.config.wrapper.SlotListPropertyWrapper;

/**
 * Defines the contract for dynamically configurable enchantment properties.
 * Implemented via mixin injection on standard enchantment definitions.
 */
public interface ConfigurableEnchantmentDefinition {
    /**
     * Sets the maximum level wrapper.
     *
     * @param wrapper The integer property wrapper.
     */
    void enchantment_core$setMaxLevelWrapper(IntPropertyWrapper wrapper);

    /**
     * Retrieves the maximum level wrapper.
     *
     * @return The integer property wrapper.
     */
    IntPropertyWrapper enchantment_core$getMaxLevelWrapper();

    /**
     * Sets the weight wrapper.
     *
     * @param wrapper The integer property wrapper.
     */
    void enchantment_core$setWeightWrapper(IntPropertyWrapper wrapper);

    /**
     * Retrieves the weight wrapper.
     *
     * @return The integer property wrapper.
     */
    IntPropertyWrapper enchantment_core$getWeightWrapper();

    /**
     * Sets the anvil cost wrapper.
     *
     * @param wrapper The integer property wrapper.
     */
    void enchantment_core$setAnvilCostWrapper(IntPropertyWrapper wrapper);

    /**
     * Retrieves the anvil cost wrapper.
     *
     * @return The integer property wrapper.
     */
    IntPropertyWrapper enchantment_core$getAnvilCostWrapper();

    /**
     * Sets the minimum cost wrapper.
     *
     * @param wrapper The cost property wrapper.
     */
    void enchantment_core$setMinCostWrapper(CostPropertyWrapper wrapper);

    /**
     * Retrieves the minimum cost wrapper.
     *
     * @return The cost property wrapper.
     */
    CostPropertyWrapper enchantment_core$getMinCostWrapper();

    /**
     * Sets the maximum cost wrapper.
     *
     * @param wrapper The cost property wrapper.
     */
    void enchantment_core$setMaxCostWrapper(CostPropertyWrapper wrapper);

    /**
     * Retrieves the maximum cost wrapper.
     *
     * @return The cost property wrapper.
     */
    CostPropertyWrapper enchantment_core$getMaxCostWrapper();

    /**
     * Sets the supported items wrapper.
     *
     * @param wrapper The item set property wrapper.
     */
    void enchantment_core$setSupportedItemsWrapper(ItemSetPropertyWrapper wrapper);

    /**
     * Retrieves the supported items wrapper.
     *
     * @return The item set property wrapper.
     */
    ItemSetPropertyWrapper enchantment_core$getSupportedItemsWrapper();

    /**
     * Sets the primary items wrapper.
     *
     * @param wrapper The item set property wrapper.
     */
    void enchantment_core$setPrimaryItemsWrapper(ItemSetPropertyWrapper wrapper);

    /**
     * Retrieves the primary items wrapper.
     *
     * @return The item set property wrapper.
     */
    ItemSetPropertyWrapper enchantment_core$getPrimaryItemsWrapper();

    /**
     * Sets the equipment slots wrapper.
     *
     * @param wrapper The slot list property wrapper.
     */
    void enchantment_core$setSlotsWrapper(SlotListPropertyWrapper wrapper);

    /**
     * Retrieves the equipment slots wrapper.
     *
     * @return The slot list property wrapper.
     */
    SlotListPropertyWrapper enchantment_core$getSlotsWrapper();
}