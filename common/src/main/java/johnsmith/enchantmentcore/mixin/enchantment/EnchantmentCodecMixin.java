package johnsmith.enchantmentcore.mixin.enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

import johnsmith.enchantmentcore.api.config.OrphanHandler;
import johnsmith.enchantmentcore.api.config.data.ItemOrItems;
import johnsmith.enchantmentcore.api.config.ConfigurableEnchantmentDefinition;
import johnsmith.enchantmentcore.api.config.wrapper.CostPropertyWrapper;
import johnsmith.enchantmentcore.api.config.wrapper.IntPropertyWrapper;
import johnsmith.enchantmentcore.api.config.wrapper.ItemSetPropertyWrapper;
import johnsmith.enchantmentcore.api.config.wrapper.SlotListPropertyWrapper;
import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.config.property.EnchantableItemListProperty;
import johnsmith.enchantmentcore.config.property.SlotListProperty;
import johnsmith.enchantmentcore.config.registry.EnchantmentCore$DynamicPropertyTypeRegistry;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting the static initialization of the Enchantment class.
 * <p>
 * Purpose: This mixin intercepts and replaces the static {@code DIRECT_CODEC} of the {@link Enchantment} class.
 * The replacement codec acts as a "man-in-the-middle" to enable dynamic, JSON-driven configuration for
 * standard enchantment properties (e.g., max_level, weight, anvil_cost). It intercepts custom JSON configuration
 * blocks before passing the sanitized data to the original Vanilla codec, and reconstructs those configuration
 * blocks when serializing the data back to disk.
 */
@Mixin(Enchantment.class)
public class EnchantmentCodecMixin {

    /**
     * Shadow reference to the vanilla Enchantment codec.
     * Marked as Mutable to allow reassignment during the static initialization phase.
     */
    @Shadow @Final @Mutable public static Codec<Enchantment> DIRECT_CODEC;

    /**
     * Injects logic at the end (TAIL) of the {@code <clinit>} (static initializer block) of the Enchantment class.
     * This guarantees that Vanilla has finished constructing the original DIRECT_CODEC before we capture and wrap it.
     *
     * @param ci Callback info representing the static initialization state.
     */
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void interceptCodec(CallbackInfo ci) {
        // Capture the fully constructed vanilla codec.
        Codec<Enchantment> original = DIRECT_CODEC;

        // Reassign the static field to our custom wrapping codec.
        DIRECT_CODEC = new Codec<Enchantment>() {

            /**
             * Intercepts the deserialization (loading from JSON/NBT) pipeline.
             * It extracts custom configuration definitions, leaves behind the "fallback" values for Vanilla to parse,
             * and then applies the live configuration bindings to the resulting Enchantment object.
             */
            @Override
            public <T> DataResult<Pair<Enchantment, T>> decode(DynamicOps<T> ops, T input) {
                Dynamic<T> dynamic = new Dynamic<>(ops, input);

                // --- 1. EXTRACTION PHASE ---
                // Parse and strip custom configuration blocks from the JSON tree for primitive integers.
                Pair<Dynamic<T>, IntPropertyWrapper> maxLevelData = parsePrimitive(dynamic, ops, "max_level", 1, 1, 255);
                if (maxLevelData.getSecond() != null) dynamic = maxLevelData.getFirst();

                Pair<Dynamic<T>, IntPropertyWrapper> weightData = parsePrimitive(dynamic, ops, "weight", 1, 1, 1024);
                if (weightData.getSecond() != null) dynamic = weightData.getFirst();

                Pair<Dynamic<T>, IntPropertyWrapper> anvilCostData = parsePrimitive(dynamic, ops, "anvil_cost", 0, 0, Integer.MAX_VALUE);
                if (anvilCostData.getSecond() != null) dynamic = anvilCostData.getFirst();

                // Parse and strip custom configuration blocks for nested Cost objects.
                Pair<Dynamic<T>, CostPropertyWrapper> minCostData = parseCost(dynamic, ops, "min_cost");
                if (minCostData.getSecond() != null) dynamic = minCostData.getFirst();

                Pair<Dynamic<T>, CostPropertyWrapper> maxCostData = parseCost(dynamic, ops, "max_cost");
                if (maxCostData.getSecond() != null) dynamic = maxCostData.getFirst();

                // Parse and strip custom configuration blocks for Item/Tag list resolution.
                Pair<Dynamic<T>, ItemSetPropertyWrapper> supportedItemsData = parseItemSet(dynamic, ops, "supported_items");
                if (supportedItemsData.getSecond() != null) dynamic = supportedItemsData.getFirst();

                Pair<Dynamic<T>, ItemSetPropertyWrapper> primaryItemsData = parseItemSet(dynamic, ops, "primary_items");
                if (primaryItemsData.getSecond() != null) dynamic = primaryItemsData.getFirst();

                // Parse and strip custom configuration blocks for Equipment Slots.
                Pair<Dynamic<T>, SlotListPropertyWrapper> slotsData = parseSlots(dynamic, ops, "slots");
                if (slotsData.getSecond() != null) dynamic = slotsData.getFirst();

                // --- 2. DELEGATION PHASE ---
                // Pass the sanitized Dynamic object (containing only Vanilla-compliant data) to the original codec.
                DataResult<Pair<Enchantment, T>> decodedResult = original.decode(ops, dynamic.getValue());

                // --- 3. INJECTION PHASE ---
                // If Vanilla successfully parsed the enchantment, cast it to our injected duck interface
                // and assign the live configuration wrappers.
                if (decodedResult.result().isPresent()) {
                    Enchantment enchantment = decodedResult.result().get().getFirst();
                    ConfigurableEnchantmentDefinition configurable = (ConfigurableEnchantmentDefinition) (Object) enchantment;

                    if (maxLevelData.getSecond() != null) configurable.enchantment_core$setMaxLevelWrapper(maxLevelData.getSecond());
                    if (weightData.getSecond() != null) configurable.enchantment_core$setWeightWrapper(weightData.getSecond());
                    if (anvilCostData.getSecond() != null) configurable.enchantment_core$setAnvilCostWrapper(anvilCostData.getSecond());

                    if (minCostData.getSecond() != null && minCostData.getSecond().isConfigured()) configurable.enchantment_core$setMinCostWrapper(minCostData.getSecond());
                    if (maxCostData.getSecond() != null && maxCostData.getSecond().isConfigured()) configurable.enchantment_core$setMaxCostWrapper(maxCostData.getSecond());

                    if (supportedItemsData.getSecond() != null) configurable.enchantment_core$setSupportedItemsWrapper(supportedItemsData.getSecond());
                    if (primaryItemsData.getSecond() != null) configurable.enchantment_core$setPrimaryItemsWrapper(primaryItemsData.getSecond());

                    if (slotsData.getSecond() != null) configurable.enchantment_core$setSlotsWrapper(slotsData.getSecond());
                }

                return decodedResult;
            }

            /**
             * Intercepts the serialization (saving to JSON/NBT) pipeline.
             * It allows Vanilla to encode the state first, then intercepts the output and re-injects
             * the custom configuration blocks (the {"config": ..., "fallback": ...} structure) if applicable.
             */
            @Override
            public <T> DataResult<T> encode(Enchantment input, DynamicOps<T> ops, T prefix) {
                // 1. Delegate to the vanilla codec to generate the baseline encoded tree.
                DataResult<T> originalResult = original.encode(input, ops, prefix);
                ConfigurableEnchantmentDefinition configurable = (ConfigurableEnchantmentDefinition) (Object) input;

                // 2. Map over the successful result to inject our custom JSON properties.
                return originalResult.flatMap(encoded -> {
                    Dynamic<T> dynamic = new Dynamic<>(ops, encoded);

                    // Reconstruct primitive configuration wrappers.
                    dynamic = appendConfig(dynamic, ops, "max_level", configurable.enchantment_core$getMaxLevelWrapper(), input.getMaxLevel());
                    dynamic = appendConfig(dynamic, ops, "weight", configurable.enchantment_core$getWeightWrapper(), input.getWeight());
                    dynamic = appendConfig(dynamic, ops, "anvil_cost", configurable.enchantment_core$getAnvilCostWrapper(), input.getAnvilCost());

                    // Reconstruct nested Cost wrappers.
                    dynamic = appendCostConfig(dynamic, ops, "min_cost", configurable.enchantment_core$getMinCostWrapper(), input.definition().minCost());
                    dynamic = appendCostConfig(dynamic, ops, "max_cost", configurable.enchantment_core$getMaxCostWrapper(), input.definition().maxCost());

                    // Reconstruct Item/Tag list wrappers.
                    dynamic = appendItemSetConfig(dynamic, ops, "supported_items", configurable.enchantment_core$getSupportedItemsWrapper());
                    dynamic = appendItemSetConfig(dynamic, ops, "primary_items", configurable.enchantment_core$getPrimaryItemsWrapper());

                    // Reconstruct Equipment Slot wrappers.
                    dynamic = appendSlotsConfig(dynamic, ops, "slots", configurable.enchantment_core$getSlotsWrapper());

                    return DataResult.success(dynamic.getValue());
                });
            }

            // region Utility Methods: extraction (JSON -> Runtime)

            /**
             * Extracts a custom configuration block for a primitive integer field.
             * Looks for the schema: "key": {"config": {mod_id, category, ...}, "fallback": 1}
             *
             * @return A Pair containing the updated Dynamic (with the config block replaced by the fallback)
             *         and the instantiated IntPropertyWrapper. Returns null for the wrapper if no config is found.
             */
            private <T> Pair<Dynamic<T>, IntPropertyWrapper> parsePrimitive(Dynamic<T> dynamic, DynamicOps<T> ops, String key, int defaultFallback, int min, int max) {
                Optional<Dynamic<T>> opt = dynamic.get(key).result();

                // Verify the key holds an object containing a nested "config" key.
                if (opt.isPresent() && opt.get().get("config").result().isPresent()) {
                    Optional<ConfigDescription> c = ConfigDescription.CODEC.parse(opt.get().get("config").orElseEmptyMap()).result();
                    Optional<Number> f = opt.get().get("fallback").asNumber().result();

                    if (c.isPresent() && f.isPresent()) {
                        int fallback = f.get().intValue();
                        // Replace the entire {"config": ..., "fallback": ...} object with just the raw fallback integer for Vanilla.
                        dynamic = dynamic.set(key, dynamic.createInt(fallback));

                        // Resolve the live property and return the wrapper.
                        return Pair.of(dynamic, new IntPropertyWrapper(resolveProperty(c.get(), fallback, min, max), c.get()));
                    }
                }
                return Pair.of(dynamic, null);
            }

            /**
             * Extracts custom configuration blocks embedded within a compound Enchantment.Cost object.
             * Costs contain two primitives: 'base' and 'per_level_above_first'.
             */
            private <T> Pair<Dynamic<T>, CostPropertyWrapper> parseCost(Dynamic<T> dynamic, DynamicOps<T> ops, String costKey) {
                Optional<Dynamic<T>> costOpt = dynamic.get(costKey).result();
                if (costOpt.isEmpty()) return Pair.of(dynamic, null);

                Dynamic<T> costDyn = costOpt.get();
                boolean modified = false;

                // Process the 'base' property within the cost object.
                Pair<Dynamic<T>, IntPropertyWrapper> baseData = parsePrimitive(costDyn, ops, "base", 0, 0, Integer.MAX_VALUE);
                if (baseData.getSecond() != null) {
                    costDyn = baseData.getFirst();
                    modified = true;
                }

                // Process the 'per_level_above_first' property within the cost object.
                Pair<Dynamic<T>, IntPropertyWrapper> perLevelData = parsePrimitive(costDyn, ops, "per_level_above_first", 0, 0, Integer.MAX_VALUE);
                if (perLevelData.getSecond() != null) {
                    costDyn = perLevelData.getFirst();
                    modified = true;
                }

                // If either sub-property was configured, update the parent Dynamic and return the wrapper.
                if (modified) {
                    dynamic = dynamic.set(costKey, costDyn);
                    return Pair.of(dynamic, new CostPropertyWrapper(baseData.getSecond(), perLevelData.getSecond()));
                }

                return Pair.of(dynamic, null);
            }

            /**
             * Extracts custom configuration blocks mapping to a list of Items or Tags.
             * Schema: "key": {"config": {...}, "fallback": ["minecraft:stick", "#minecraft:swords"]}
             */
            private <T> Pair<Dynamic<T>, ItemSetPropertyWrapper> parseItemSet(Dynamic<T> dynamic, DynamicOps<T> ops, String key) {
                Optional<Dynamic<T>> opt = dynamic.get(key).result();

                if (opt.isPresent() && opt.get().get("config").result().isPresent()) {
                    Optional<ConfigDescription> c = ConfigDescription.CODEC.parse(opt.get().get("config").orElseEmptyMap()).result();
                    Optional<Dynamic<T>> f = opt.get().get("fallback").result();

                    if (c.isPresent() && f.isPresent()) {
                        Dynamic<T> fallbackDyn = f.get();
                        // Extract the raw Java list of items/tags from the dynamic fallback structure.
                        List<ItemOrItems> fallbackItems = extractItems(fallbackDyn, ops);

                        // Edge Case: If the fallback is a single Item Tag, we must serialize it explicitly
                        // into the parent Dynamic to satisfy Vanilla's parsing expectations.
                        if (fallbackItems.size() == 1 && fallbackItems.get(0).isTag()) {
                            DataResult<T> encoded = ItemOrItems.CODEC.encodeStart(ops, fallbackItems.get(0));
                            if (encoded.result().isPresent()) {
                                fallbackDyn = new Dynamic<>(ops, encoded.result().get());
                            }
                        }

                        // Set the sanitized fallback back into the primary tree.
                        dynamic = dynamic.set(key, fallbackDyn);
                        return Pair.of(dynamic, new ItemSetPropertyWrapper(resolveItemListProperty(c.get(), fallbackItems), c.get(), fallbackItems));
                    }
                }
                return Pair.of(dynamic, null);
            }

            /**
             * Helper to parse a Dynamic list or single string into a List of ItemOrItems instances.
             */
            private <T> List<ItemOrItems> extractItems(Dynamic<T> dynamic, DynamicOps<T> ops) {
                // Handle the case where the input is a single string rather than a list array.
                if (dynamic.asString().result().isPresent()) {
                    Optional<ItemOrItems> parsed = ItemOrItems.CODEC.parse(ops, dynamic.getValue()).result();
                    if (parsed.isPresent()) {
                        return List.of(parsed.get());
                    }
                }
                // Handle the standard JSON array case.
                return dynamic.asList(d -> ItemOrItems.CODEC.parse(ops, d.getValue()).result().orElse(null))
                        .stream().filter(Objects::nonNull).toList();
            }

            /**
             * Extracts custom configuration blocks mapping to a list of EquipmentSlotGroups.
             */
            private <T> Pair<Dynamic<T>, SlotListPropertyWrapper> parseSlots(Dynamic<T> dynamic, DynamicOps<T> ops, String key) {
                Optional<Dynamic<T>> opt = dynamic.get(key).result();

                if (opt.isPresent() && opt.get().get("config").result().isPresent()) {
                    Optional<ConfigDescription> c = ConfigDescription.CODEC.parse(opt.get().get("config").orElseEmptyMap()).result();
                    Optional<Dynamic<T>> f = opt.get().get("fallback").result();

                    if (c.isPresent() && f.isPresent()) {
                        Dynamic<T> fallbackDyn = f.get();
                        List<EquipmentSlotGroup> fallbackSlots = extractSlotGroups(fallbackDyn);

                        dynamic = dynamic.set(key, fallbackDyn);
                        return Pair.of(dynamic, new SlotListPropertyWrapper(resolveSlotListProperty(c.get(), fallbackSlots), c.get(), fallbackSlots));
                    }
                }
                return Pair.of(dynamic, null);
            }

            /**
             * Helper to parse a Dynamic object into a List of EquipmentSlotGroup enums.
             */
            private <T> List<EquipmentSlotGroup> extractSlotGroups(Dynamic<T> dynamic) {
                DataResult<List<EquipmentSlotGroup>> result = EquipmentSlotGroup.CODEC.listOf().parse(dynamic);
                if (result.result().isPresent()) {
                    return result.result().get();
                }
                DataResult<EquipmentSlotGroup> single = EquipmentSlotGroup.CODEC.parse(dynamic);
                if (single.result().isPresent()) {
                    return List.of(single.result().get());
                }
                return List.of(); // Empty fallback
            }

            // endregion

            // region Property Resolution & Orphan Handling

            /**
             * Links the requested configuration description to a live Config Overhauled property instance.
             * Adopts the property into an Orphan category if the requested ModId is not loaded.
             */
            @SuppressWarnings("unchecked")
            private Property<Integer> resolveProperty(ConfigDescription config, int fallback, int min, int max) {
                ConfigManager manager = ConfigRegistry.getManager(config.modId());
                if (manager != null) {
                    return (Property<Integer>) manager.getOrCreateDynamicProperty(config, DynamicPropertyTypeRegistry.INTEGER, fallback, min, max);
                }
                return (Property<Integer>) Config.MANAGER.getOrCreateDynamicProperty(OrphanHandler.Provider.get().adopt(config), DynamicPropertyTypeRegistry.INTEGER, fallback, min, max);
            }

            /**
             * Resolves a live property for an EnchantableItemList type.
             */
            @SuppressWarnings("unchecked")
            private EnchantableItemListProperty resolveItemListProperty(ConfigDescription config, List<ItemOrItems> fallback) {
                ConfigManager manager = ConfigRegistry.getManager(config.modId());
                if (manager != null) {
                    return (EnchantableItemListProperty) manager.getOrCreateDynamicProperty(config, EnchantmentCore$DynamicPropertyTypeRegistry.ENCHANTABLE_ITEM_LIST, fallback, null, null);
                }
                return (EnchantableItemListProperty) Config.MANAGER.getOrCreateDynamicProperty(OrphanHandler.Provider.get().adopt(config), EnchantmentCore$DynamicPropertyTypeRegistry.ENCHANTABLE_ITEM_LIST, fallback, null, null);
            }

            /**
             * Resolves a live property for a SlotList type.
             */
            @SuppressWarnings("unchecked")
            private SlotListProperty resolveSlotListProperty(ConfigDescription config, List<EquipmentSlotGroup> fallback) {
                ConfigManager manager = ConfigRegistry.getManager(config.modId());
                if (manager != null) {
                    return (SlotListProperty) manager.getOrCreateDynamicProperty(config, EnchantmentCore$DynamicPropertyTypeRegistry.SLOT_LIST, fallback, null, null);
                }
                return (SlotListProperty) Config.MANAGER.getOrCreateDynamicProperty(OrphanHandler.Provider.get().adopt(config), EnchantmentCore$DynamicPropertyTypeRegistry.SLOT_LIST, fallback, null, null);
            }

            // endregion

            // region Utility Methods: Reconstruction (Runtime -> JSON)

            /**
             * Appends a custom configuration block back into the Dynamic tree during serialization
             * if a live configuration wrapper is actively attached to the Enchantment.
             */
            private <T> Dynamic<T> appendConfig(Dynamic<T> dynamic, DynamicOps<T> ops, String key, IntPropertyWrapper wrapper, int fallback) {
                if (wrapper == null || !wrapper.isConfigured()) return dynamic;

                // Encode the ConfigDescription object identifier into the output.
                Optional<T> configObjOpt = ConfigDescription.CODEC.encodeStart(ops, wrapper.description()).result();

                if (configObjOpt.isPresent()) {
                    // Overwrite the primitive value with the nested object structure.
                    return dynamic.set(key, dynamic.emptyMap()
                            .set("config", new Dynamic<>(ops, configObjOpt.get()))
                            .set("fallback", dynamic.createInt(fallback)));
                }
                return dynamic;
            }

            /**
             * Iterates over a compound Enchantment.Cost object to selectively re-inject
             * configuration blocks to the 'base' and 'per_level_above_first' attributes.
             */
            private <T> Dynamic<T> appendCostConfig(Dynamic<T> dynamic, DynamicOps<T> ops, String costKey, CostPropertyWrapper wrapper, Enchantment.Cost fallbackCost) {
                if (wrapper == null || !wrapper.isConfigured()) return dynamic;

                Optional<Dynamic<T>> costOpt = dynamic.get(costKey).result();
                if (costOpt.isEmpty()) return dynamic;
                Dynamic<T> costDyn = costOpt.get();

                costDyn = appendConfig(costDyn, ops, "base", wrapper.baseWrapper(), fallbackCost.base());
                costDyn = appendConfig(costDyn, ops, "per_level_above_first", wrapper.perLevelWrapper(), fallbackCost.perLevelAboveFirst());

                return dynamic.set(costKey, costDyn);
            }

            /**
             * Appends a custom configuration block for Item Lists back into the Dynamic tree.
             */
            private <T> Dynamic<T> appendItemSetConfig(Dynamic<T> dynamic, DynamicOps<T> ops, String key, ItemSetPropertyWrapper wrapper) {
                if (wrapper == null || !wrapper.isConfigured()) return dynamic;

                Optional<T> configObjOpt = ConfigDescription.CODEC.encodeStart(ops, wrapper.description()).result();

                if (configObjOpt.isPresent()) {
                    List<Dynamic<T>> fallbackDyns = new ArrayList<>();

                    // Manually serialize the fallback items into the dynamic format.
                    for (ItemOrItems entry : wrapper.fallbackItems()) {
                        DataResult<T> encoded = ItemOrItems.CODEC.encodeStart(ops, entry);
                        encoded.result().ifPresent(val -> fallbackDyns.add(new Dynamic<>(ops, val)));
                    }

                    Dynamic<T> fallbackDyn;

                    // Specific edge-case: If the array only contains a single tag element, preserve it as a single node
                    // to match the expected Vanilla schema syntax.
                    if (fallbackDyns.size() == 1 && wrapper.fallbackItems().get(0).isTag()) {
                        fallbackDyn = fallbackDyns.get(0);
                    } else {
                        // Otherwise, wrap it in a proper JSON array structure.
                        fallbackDyn = dynamic.createList(fallbackDyns.stream());
                    }

                    return dynamic.set(key, dynamic.emptyMap()
                            .set("config", new Dynamic<>(ops, configObjOpt.get()))
                            .set("fallback", fallbackDyn));
                }
                return dynamic;
            }

            /**
             * Appends a custom configuration block for Equipment Slots back into the Dynamic tree.
             */
            private <T> Dynamic<T> appendSlotsConfig(Dynamic<T> dynamic, DynamicOps<T> ops, String key, SlotListPropertyWrapper wrapper) {
                if (wrapper == null || !wrapper.isConfigured()) return dynamic;

                Optional<T> configObjOpt = ConfigDescription.CODEC.encodeStart(ops, wrapper.description()).result();

                if (configObjOpt.isPresent()) {
                    DataResult<T> fallbackEncoded = EquipmentSlotGroup.CODEC.listOf().encodeStart(ops, wrapper.fallback());

                    if (fallbackEncoded.result().isPresent()) {
                        return dynamic.set(key, dynamic.emptyMap()
                                .set("config", new Dynamic<>(ops, configObjOpt.get()))
                                .set("fallback", new Dynamic<>(ops, fallbackEncoded.result().get())));
                    }
                }
                return dynamic;
            }

            // endregion
        };
    }
}