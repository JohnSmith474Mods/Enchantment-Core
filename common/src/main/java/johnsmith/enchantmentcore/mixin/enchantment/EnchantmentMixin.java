package johnsmith.enchantmentcore.mixin.enchantment;

import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.api.config.ConfigurableEnchantmentDefinition;
import johnsmith.enchantmentcore.api.config.wrapper.CostPropertyWrapper;
import johnsmith.enchantmentcore.api.config.wrapper.IntPropertyWrapper;
import johnsmith.enchantmentcore.api.config.wrapper.ItemSetPropertyWrapper;
import johnsmith.enchantmentcore.api.config.wrapper.SlotListPropertyWrapper;

import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting the base Enchantment class.
 * Implements the ConfigurableEnchantmentDefinition interface to hold configuration wrapper state.
 * Intercepts core property accessors to inject dynamic runtime configuration values.
 * Rebuilds the EnchantmentDefinition record dynamically when configuration state deviates from the vanilla baseline.
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin implements ConfigurableEnchantmentDefinition {

    /**
     * Shadow reference to the vanilla enchantment definition record.
     */
    @Shadow
    @Final
    private Enchantment.EnchantmentDefinition definition;

    /** Configuration wrapper controlling the maximum applicable level of the enchantment. */
    @Unique private IntPropertyWrapper enchantment_core$maxLevelWrapper;
    /** Configuration wrapper controlling the generation weight of the enchantment. */
    @Unique private IntPropertyWrapper enchantment_core$weightWrapper;
    /** Configuration wrapper controlling the anvil combination cost. */
    @Unique private IntPropertyWrapper enchantment_core$anvilCostWrapper;
    /** Configuration wrapper controlling the minimum enchanting power cost. */
    @Unique private CostPropertyWrapper enchantment_core$minCostWrapper;
    /** Configuration wrapper controlling the maximum enchanting power cost. */
    @Unique private CostPropertyWrapper enchantment_core$maxCostWrapper;
    /** Configuration wrapper controlling the total set of items compatible with this enchantment. */
    @Unique private ItemSetPropertyWrapper enchantment_core$supportedItemsWrapper;
    /** Configuration wrapper controlling the primary set of items preferred by this enchantment. */
    @Unique private ItemSetPropertyWrapper enchantment_core$primaryItemsWrapper;
    /** Configuration wrapper controlling the valid equipment slots where the enchantment operates. */
    @Unique private SlotListPropertyWrapper enchantment_core$slotsWrapper;

    @Override
    public void enchantment_core$setMaxLevelWrapper(IntPropertyWrapper wrapper) { this.enchantment_core$maxLevelWrapper = wrapper; }

    @Override
    public IntPropertyWrapper enchantment_core$getMaxLevelWrapper() { return this.enchantment_core$maxLevelWrapper; }

    @Override
    public void enchantment_core$setWeightWrapper(IntPropertyWrapper wrapper) { this.enchantment_core$weightWrapper = wrapper; }

    @Override
    public IntPropertyWrapper enchantment_core$getWeightWrapper() { return this.enchantment_core$weightWrapper; }

    @Override
    public void enchantment_core$setAnvilCostWrapper(IntPropertyWrapper wrapper) { this.enchantment_core$anvilCostWrapper = wrapper; }

    @Override
    public IntPropertyWrapper enchantment_core$getAnvilCostWrapper() { return this.enchantment_core$anvilCostWrapper; }

    @Override
    public void enchantment_core$setMinCostWrapper(CostPropertyWrapper wrapper) { this.enchantment_core$minCostWrapper = wrapper; }

    @Override
    public CostPropertyWrapper enchantment_core$getMinCostWrapper() { return this.enchantment_core$minCostWrapper; }

    @Override
    public void enchantment_core$setMaxCostWrapper(CostPropertyWrapper wrapper) { this.enchantment_core$maxCostWrapper = wrapper; }

    @Override
    public CostPropertyWrapper enchantment_core$getMaxCostWrapper() { return this.enchantment_core$maxCostWrapper; }

    @Override
    public void enchantment_core$setSupportedItemsWrapper(ItemSetPropertyWrapper wrapper) { this.enchantment_core$supportedItemsWrapper = wrapper; }

    @Override
    public ItemSetPropertyWrapper enchantment_core$getSupportedItemsWrapper() { return this.enchantment_core$supportedItemsWrapper; }

    @Override
    public void enchantment_core$setPrimaryItemsWrapper(ItemSetPropertyWrapper wrapper) { this.enchantment_core$primaryItemsWrapper = wrapper; }

    @Override
    public ItemSetPropertyWrapper enchantment_core$getPrimaryItemsWrapper() { return this.enchantment_core$primaryItemsWrapper; }

    @Override
    public void enchantment_core$setSlotsWrapper(SlotListPropertyWrapper wrapper) { this.enchantment_core$slotsWrapper = wrapper; }

    @Override
    public SlotListPropertyWrapper enchantment_core$getSlotsWrapper() { return this.enchantment_core$slotsWrapper; }

    /**
     * Intercepts the definition accessor.
     * Evaluates all configuration wrappers against the original definition.
     * Generates and returns a modified definition record if any active configuration differs from the baseline.
     *
     * @param cir The callback information carrying the EnchantmentDefinition return value.
     */
    @Inject(method = "definition()Lnet/minecraft/world/item/enchantment/Enchantment$EnchantmentDefinition;", at = @At("HEAD"), cancellable = true)
    private void onDefinition(CallbackInfoReturnable<Enchantment.EnchantmentDefinition> cir) {
        boolean needsRebuild = false;

        // Resolve primitive integer parameters.
        int currentMax = this.enchantment_core$maxLevelWrapper != null ? this.enchantment_core$maxLevelWrapper.resolve(this.definition.maxLevel()) : this.definition.maxLevel();
        int currentWeight = this.enchantment_core$weightWrapper != null ? this.enchantment_core$weightWrapper.resolve(this.definition.weight()) : this.definition.weight();
        int currentAnvilCost = this.enchantment_core$anvilCostWrapper != null ? this.enchantment_core$anvilCostWrapper.resolve(this.definition.anvilCost()) : this.definition.anvilCost();

        // Flag rebuild if any primitive parameter differs from the vanilla definition.
        if (currentMax != this.definition.maxLevel() || currentWeight != this.definition.weight() || currentAnvilCost != this.definition.anvilCost()) {
            needsRebuild = true;
        }

        // Resolve compound cost parameters.
        Enchantment.Cost currentMinCost = this.definition.minCost();
        if (this.enchantment_core$minCostWrapper != null && this.enchantment_core$minCostWrapper.isDifferent(currentMinCost)) {
            currentMinCost = this.enchantment_core$minCostWrapper.resolve(currentMinCost);
            needsRebuild = true;
        }

        Enchantment.Cost currentMaxCost = this.definition.maxCost();
        if (this.enchantment_core$maxCostWrapper != null && this.enchantment_core$maxCostWrapper.isDifferent(currentMaxCost)) {
            currentMaxCost = this.enchantment_core$maxCostWrapper.resolve(currentMaxCost);
            needsRebuild = true;
        }

        // Resolve supported item sets.
        HolderSet<Item> currentSupported = this.definition.supportedItems();
        if (this.enchantment_core$supportedItemsWrapper != null && this.enchantment_core$supportedItemsWrapper.isConfigured()) {
            currentSupported = this.enchantment_core$supportedItemsWrapper.resolve(currentSupported);
            needsRebuild = true;
        }

        // Resolve primary item sets. Fallback to supported items if primary is undefined.
        Optional<HolderSet<Item>> currentPrimary = this.definition.primaryItems();
        if (this.enchantment_core$primaryItemsWrapper != null && this.enchantment_core$primaryItemsWrapper.isConfigured()) {
            HolderSet<Item> resolvedPrimary = this.enchantment_core$primaryItemsWrapper.resolve(currentPrimary.orElse(currentSupported));
            currentPrimary = Optional.of(resolvedPrimary);
            needsRebuild = true;
        }

        // Resolve equipment slot groups.
        List<EquipmentSlotGroup> currentSlots = this.definition.slots();
        if (this.enchantment_core$slotsWrapper != null && this.enchantment_core$slotsWrapper.isConfigured()) {
            List<EquipmentSlotGroup> resolvedSlots = this.enchantment_core$slotsWrapper.resolve(currentSlots);
            if (!resolvedSlots.equals(currentSlots)) {
                currentSlots = resolvedSlots;
                needsRebuild = true;
            }
        }

        // Return a fresh definition record if modifications occurred.
        if (needsRebuild) {
            cir.setReturnValue(new Enchantment.EnchantmentDefinition(
                    currentSupported,
                    currentPrimary,
                    currentWeight,
                    currentMax,
                    currentMinCost,
                    currentMaxCost,
                    currentAnvilCost,
                    currentSlots
            ));
        }
    }

    /**
     * Intercepts direct accessor for maximum level.
     *
     * @param cir The callback information.
     */
    @Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true)
    private void onGetMaxLevel(CallbackInfoReturnable<Integer> cir) {
        if (this.enchantment_core$maxLevelWrapper != null && this.enchantment_core$maxLevelWrapper.isConfigured()) {
            cir.setReturnValue(this.enchantment_core$maxLevelWrapper.property().get());
        }
    }

    /**
     * Intercepts direct accessor for weight.
     *
     * @param cir The callback information.
     */
    @Inject(method = "getWeight", at = @At("HEAD"), cancellable = true)
    private void onGetWeight(CallbackInfoReturnable<Integer> cir) {
        if (this.enchantment_core$weightWrapper != null && this.enchantment_core$weightWrapper.isConfigured()) {
            cir.setReturnValue(this.enchantment_core$weightWrapper.property().get());
        }
    }

    /**
     * Intercepts direct accessor for anvil cost.
     *
     * @param cir The callback information.
     */
    @Inject(method = "getAnvilCost", at = @At("HEAD"), cancellable = true)
    private void onGetAnvilCost(CallbackInfoReturnable<Integer> cir) {
        if (this.enchantment_core$anvilCostWrapper != null && this.enchantment_core$anvilCostWrapper.isConfigured()) {
            cir.setReturnValue(this.enchantment_core$anvilCostWrapper.property().get());
        }
    }

    /**
     * Intercepts direct accessor for supported items. Routes request through the dynamically constructed definition.
     *
     * @param cir The callback information.
     */
    @Inject(method = "getSupportedItems", at = @At("HEAD"), cancellable = true)
    private void onGetSupportedItems(CallbackInfoReturnable<HolderSet<Item>> cir) {
        cir.setReturnValue(((Enchantment) (Object) this).definition().supportedItems());
    }

    /**
     * Intercepts evaluation of item support.
     * Validates the item stack against the configured item set wrapper.
     *
     * @param stack The item stack to evaluate.
     * @param cir   The callback information.
     */
    @Inject(method = "isSupportedItem", at = @At("HEAD"), cancellable = true)
    private void onIsSupportedItem(net.minecraft.world.item.ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.enchantment_core$supportedItemsWrapper != null && this.enchantment_core$supportedItemsWrapper.isConfigured()) {
            cir.setReturnValue(stack.is(this.enchantment_core$supportedItemsWrapper.resolve(this.definition.supportedItems())));
        }
    }

    /**
     * Intercepts evaluation of primary item compatibility.
     * Validates the item stack against the configured primary item set wrapper.
     *
     * @param stack The item stack to evaluate.
     * @param cir   The callback information.
     */
    @Inject(method = "isPrimaryItem", at = @At("HEAD"), cancellable = true)
    private void onIsPrimaryItem(net.minecraft.world.item.ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.enchantment_core$primaryItemsWrapper != null && this.enchantment_core$primaryItemsWrapper.isConfigured()) {
            HolderSet<Item> primary = this.enchantment_core$primaryItemsWrapper.resolve(this.definition.primaryItems().orElse(this.definition.supportedItems()));
            cir.setReturnValue(stack.is(primary));
        }
    }

    /**
     * Intercepts the general enchantment validation routine. Routes request through the dynamically constructed definition.
     *
     * @param stack The item stack to evaluate.
     * @param cir   The callback information.
     */
    @Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
    private void onCanEnchant(net.minecraft.world.item.ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(((Enchantment) (Object) this).definition().supportedItems().contains(stack.getItemHolder()));
    }
}