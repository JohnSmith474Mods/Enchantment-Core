package johnsmith.enchantmentcore.api.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Centralized registry managing the bidirectional transformation of enchantment JSON definitions
 * between static datapack formats and dynamic Config Overhauled configuration wrappers.
 * <p>
 * During the {@code /enchantment_core import} phase, registered {@link Transformer}s intercept static
 * primitive fields and {@link net.minecraft.world.item.enchantment.LevelBasedValue} nodes and wrap them
 * in configuration-bound JSON objects. During the {@code /enchantment_core export} phase, the transformers
 * strip those dynamic wrappers and restore standard vanilla-compatible JSON structures.
 */
public class DataTransformerRegistry {
    /** JSON key for the enchantment's maximum level definition. */
    public static final String KEY_MAX_LEVEL = "max_level";

    /** JSON key for the enchantment's rarity weight definition. */
    public static final String KEY_WEIGHT = "weight";

    /** JSON key for the enchantment's anvil combination cost definition. */
    public static final String KEY_ANVIL_COST = "anvil_cost";

    /** JSON key for the enchantment's supported item set definition. */
    public static final String KEY_SUPPORTED_ITEMS = "supported_items";

    /** JSON key for the enchantment's primary enchanting-table item set definition. */
    public static final String KEY_PRIMARY_ITEMS = "primary_items";

    /** JSON key for the enchantment's active equipment slot groups definition. */
    public static final String KEY_SLOTS = "slots";

    /** JSON key for the enchantment's minimum enchanting-table cost definition. */
    public static final String KEY_MIN_COST = "min_cost";

    /** JSON key for the enchantment's maximum enchanting-table cost definition. */
    public static final String KEY_MAX_COST = "max_cost";

    /**
     * Contract for executing targeted bidirectional JSON transformations on an enchantment root object.
     */
    public interface Transformer {
        /**
         * Mutates the enchantment JSON tree during the import phase, wrapping static values into
         * dynamic configuration structures bound to {@code targetNamespace}.
         *
         * @param root            The root enchantment {@link JsonObject} being mutated.
         * @param targetNamespace The mod namespace assigned to the generated configuration properties.
         * @param enchantmentName The path identifier of the target enchantment (used as the config group name).
         */
        void applyImport(JsonObject root, String targetNamespace, String enchantmentName);

        /**
         * Mutates the enchantment JSON tree during the export phase, unwrapping dynamic configuration
         * structures back into their static fallback representations.
         *
         * @param root The root enchantment {@link JsonObject} being mutated.
         */
        void applyExport(JsonObject root);
    }

    /** Ordered list of registered structural transformers executed during import and export. */
    private static final List<Transformer> TRANSFORMERS = new ArrayList<>();

    /** Set of JSON property keys known to hold {@link net.minecraft.world.item.enchantment.LevelBasedValue} definitions. */
    private static final Set<String> EFFECT_PROPERTIES = new HashSet<>();

    /** Static set of effect {@code "type"} identifiers excluded from recursive transformation. */
    private static final Set<String> TYPE_EXCLUSIONS = new HashSet<>(Set.of(
            "minecraft:play_sound",
            "minecraft:spawn_particles"
    ));

    /** Static set of structural JSON keys (such as loot predicate blocks) excluded from recursive traversal. */
    private static final Set<String> STRUCTURAL_EXCLUSIONS = new HashSet<>(Set.of(
            "requirements"
    ));

    /** Dynamic suppliers providing additional runtime effect type exclusions (e.g., from user configuration). */
    private static final List<Supplier<? extends Collection<String>>> DYNAMIC_TYPE_EXCLUSIONS = new ArrayList<>();

    /** Dynamic suppliers providing additional runtime structural key exclusions (e.g., from user configuration). */
    private static final List<Supplier<? extends Collection<String>>> DYNAMIC_STRUCTURAL_EXCLUSIONS = new ArrayList<>();

    /**
     * Registers a structural {@link Transformer} to the import/export execution pipeline.
     *
     * @param transformer The {@link Transformer} implementation to add.
     */
    public static void registerTransformer(Transformer transformer) {
        TRANSFORMERS.add(transformer);
    }

    /**
     * Registers a JSON property key identifying a field that holds {@link net.minecraft.world.item.enchantment.LevelBasedValue} data.
     *
     * @param propertyKey The exact JSON key string to target during recursive effect scanning.
     */
    public static void registerEffectProperty(String propertyKey) {
        EFFECT_PROPERTIES.add(propertyKey);
    }

    /**
     * Registers an effect {@code "type"} identifier to bypass recursive transformation.
     * <p>
     * Prevents DataFixerUpper parse errors when an effect uses identically named keys (such as {@code "volume"}
     * or {@code "pitch"}) for non-{@code LevelBasedValue} types like {@code FloatProvider}.
     *
     * @param typeId The resource location string of the effect type to exclude.
     */
    public static void registerTypeExclusion(String typeId) {
        TYPE_EXCLUSIONS.add(typeId);
    }

    /**
     * Registers multiple effect {@code "type"} identifiers to bypass recursive transformation.
     *
     * @param typeIds A collection of effect type resource location strings to exclude.
     */
    public static void registerTypeExclusions(Collection<String> typeIds) {
        TYPE_EXCLUSIONS.addAll(typeIds);
    }

    /**
     * Registers a dynamic supplier of effect type exclusions (such as a live configuration list property).
     *
     * @param supplier A supplier returning a collection of effect type identifiers to exclude.
     */
    public static void registerDynamicTypeExclusions(Supplier<? extends Collection<String>> supplier) {
        DYNAMIC_TYPE_EXCLUSIONS.add(supplier);
    }

    /**
     * Registers a JSON key identifying a structural object or array node to bypass during recursive traversal.
     * <p>
     * Isolates the transformation pipeline from independent subtrees (like loot condition {@code "requirements"})
     * that may contain identically named keys.
     *
     * @param nodeKey The exact JSON key string of the structural node to skip.
     */
    public static void registerStructuralExclusion(String nodeKey) {
        STRUCTURAL_EXCLUSIONS.add(nodeKey);
    }

    /**
     * Registers multiple JSON keys identifying structural nodes to bypass during recursive traversal.
     *
     * @param nodeKeys A collection of structural JSON key strings to skip.
     */
    public static void registerStructuralExclusions(Collection<String> nodeKeys) {
        STRUCTURAL_EXCLUSIONS.addAll(nodeKeys);
    }

    /**
     * Registers a dynamic supplier of structural node exclusions (such as a live configuration list property).
     *
     * @param supplier A supplier returning a collection of JSON node keys to exclude.
     */
    public static void registerDynamicStructuralExclusions(Supplier<? extends Collection<String>> supplier) {
        DYNAMIC_STRUCTURAL_EXCLUSIONS.add(supplier);
    }

    /**
     * Retrieves the list of registered structural {@link Transformer}s.
     *
     * @return The active list of transformers.
     */
    public static List<Transformer> getTransformers() {
        return TRANSFORMERS;
    }

    /**
     * Retrieves the set of registered JSON property keys targeted for {@link net.minecraft.world.item.enchantment.LevelBasedValue} transformation.
     *
     * @return The set of effect property keys.
     */
    public static Set<String> getEffectProperties() {
        return EFFECT_PROPERTIES;
    }

    /**
     * Retrieves the consolidated set of effect type exclusions, combining static registrations with all
     * dynamic exclusion suppliers.
     *
     * @return The consolidated set of excluded effect type identifiers.
     */
    public static Set<String> getTypeExclusions() {
        Set<String> exclusions = new HashSet<>(TYPE_EXCLUSIONS);
        for (Supplier<? extends Collection<String>> supplier : DYNAMIC_TYPE_EXCLUSIONS) {
            Collection<String> dynamic = supplier.get();
            if (dynamic != null) {
                exclusions.addAll(dynamic);
            }
        }
        return exclusions;
    }

    /**
     * Retrieves the consolidated set of structural node exclusions, combining static registrations with all
     * dynamic exclusion suppliers.
     *
     * @return The consolidated set of excluded structural JSON keys.
     */
    public static Set<String> getStructuralExclusions() {
        Set<String> exclusions = new HashSet<>(STRUCTURAL_EXCLUSIONS);
        for (Supplier<? extends Collection<String>> supplier : DYNAMIC_STRUCTURAL_EXCLUSIONS) {
            Collection<String> dynamic = supplier.get();
            if (dynamic != null) {
                exclusions.addAll(dynamic);
            }
        }
        return exclusions;
    }

    /**
     * Recursively traverses an exported JSON tree and collapses verbose constant objects
     * ({@code {"type": "minecraft:constant", "value": X}}) into compact numeric JSON primitives.
     *
     * @param element The JSON element tree to process and mutate in place.
     */
    public static void flattenExportedConstants(JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonElement child = array.get(i);
                if (isConstantObject(child)) {
                    array.set(i, extractConstantValue(child));
                } else {
                    flattenExportedConstants(child);
                }
            }
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            List<String> keys = new ArrayList<>(obj.keySet());
            for (String key : keys) {
                JsonElement child = obj.get(key);
                if (isConstantObject(child)) {
                    obj.add(key, extractConstantValue(child));
                } else {
                    flattenExportedConstants(child);
                }
            }
        }
    }

    /**
     * Evaluates whether a given JSON element represents a serialized constant {@link net.minecraft.world.item.enchantment.LevelBasedValue} object.
     *
     * @param element The JSON element to inspect.
     * @return {@code true} if the element is a JSON object with {@code "type"} equal to
     *         {@code "minecraft:constant"} or {@code "enchantment_core:configurable_constant"}, {@code false} otherwise.
     */
    private static boolean isConstantObject(JsonElement element) {
        if (!element.isJsonObject()) return false;
        JsonObject obj = element.getAsJsonObject();
        if (!obj.has("type")) return false;
        String type = obj.get("type").getAsString();
        return type.equals("minecraft:constant") || type.equals("enchantment_core:configurable_constant");
    }

    /**
     * Extracts the underlying numeric primitive from a serialized constant object.
     *
     * @param element The constant JSON object.
     * @return A {@link JsonPrimitive} holding the extracted float value, defaulting to {@code 0.0F} if missing.
     */
    private static JsonPrimitive extractConstantValue(JsonElement element) {
        JsonObject obj = element.getAsJsonObject();
        if (obj.has("default_value")) {
            return obj.getAsJsonPrimitive("default_value");
        } else if (obj.has("value")) {
            return obj.getAsJsonPrimitive("value");
        }
        return new JsonPrimitive(0.0F);
    }

    static {
        registerTransformer(new FieldWrapperTransformer(KEY_MAX_LEVEL, "max_level"));
        registerTransformer(new FieldWrapperTransformer(KEY_WEIGHT, "weight"));
        registerTransformer(new FieldWrapperTransformer(KEY_ANVIL_COST, "anvil_cost"));
        registerTransformer(new FieldWrapperTransformer(KEY_SUPPORTED_ITEMS, "supported_items"));
        registerTransformer(new FieldWrapperTransformer(KEY_PRIMARY_ITEMS, "primary_items"));
        registerTransformer(new FieldWrapperTransformer(KEY_SLOTS, "slots"));

        registerTransformer(new CostTransformer(KEY_MIN_COST));
        registerTransformer(new CostTransformer(KEY_MAX_COST));

        registerTransformer(new EffectsTransformer());

        String[] vanillaEffectKeys = {
                "value", "amount", "min_damage", "max_damage", "min_duration", "max_duration",
                "min_amplifier", "max_amplifier", "duration", "volume", "pitch", "chance",
                "radius", "offset", "knockback_multiplier"
        };
        for (String key : vanillaEffectKeys) {
            registerEffectProperty(key);
        }
    }

    /**
     * Standard {@link Transformer} implementation for wrapping and unwrapping top-level enchantment
     * definition fields into {@code {"config": ..., "fallback": ...}} objects.
     *
     * @param key          The top-level JSON property key on the enchantment object.
     * @param propertyName The generated configuration property name.
     */
    public record FieldWrapperTransformer(String key, String propertyName) implements Transformer {
        @Override
        public void applyImport(JsonObject root, String targetNamespace, String enchantmentName) {
            if (!root.has(key)) return;
            JsonElement fallback = root.get(key);
            JsonObject wrapper = new JsonObject();
            wrapper.add("config", ValueImporterRegistry.buildConfigObject(targetNamespace, enchantmentName, propertyName));
            wrapper.add("fallback", fallback);
            root.add(key, wrapper);
        }

        @Override
        public void applyExport(JsonObject root) {
            if (root.has(key) && root.get(key).isJsonObject()) {
                JsonObject obj = root.getAsJsonObject(key);
                if (obj.has("fallback")) {
                    root.add(key, obj.get("fallback"));
                }
            }
        }
    }

    /**
     * Specialized {@link Transformer} implementation for wrapping and unwrapping the {@code base}
     * and {@code per_level_above_first} sub-fields of an enchantment cost block ({@code min_cost} or {@code max_cost}).
     *
     * @param key The top-level cost JSON key ({@code "min_cost"} or {@code "max_cost"}).
     */
    public record CostTransformer(String key) implements Transformer {
        @Override
        public void applyImport(JsonObject root, String targetNamespace, String enchantmentName) {
            if (!root.has(key) || !root.get(key).isJsonObject()) return;
            JsonObject costBlock = root.getAsJsonObject(key);

            if (costBlock.has("base")) {
                JsonElement fallbackBase = costBlock.get("base");
                JsonObject wrapperBase = new JsonObject();
                wrapperBase.add("config", ValueImporterRegistry.buildConfigObject(targetNamespace, enchantmentName, key + "_base"));
                wrapperBase.add("fallback", fallbackBase);
                costBlock.add("base", wrapperBase);
            }

            if (costBlock.has("per_level_above_first")) {
                JsonElement fallbackPerLevel = costBlock.get("per_level_above_first");
                JsonObject wrapperPerLevel = new JsonObject();
                wrapperPerLevel.add("config", ValueImporterRegistry.buildConfigObject(targetNamespace, enchantmentName, key + "_per_level"));
                wrapperPerLevel.add("fallback", fallbackPerLevel);
                costBlock.add("per_level_above_first", wrapperPerLevel);
            }
        }

        @Override
        public void applyExport(JsonObject root) {
            if (root.has(key) && root.get(key).isJsonObject()) {
                JsonObject costObj = root.getAsJsonObject(key);
                if (costObj.has("base") && costObj.get("base").isJsonObject() && costObj.getAsJsonObject("base").has("fallback")) {
                    costObj.add("base", costObj.getAsJsonObject("base").get("fallback"));
                }
                if (costObj.has("per_level_above_first") && costObj.get("per_level_above_first").isJsonObject() && costObj.getAsJsonObject("per_level_above_first").has("fallback")) {
                    costObj.add("per_level_above_first", costObj.getAsJsonObject("per_level_above_first").get("fallback"));
                }
            }
        }
    }

    /**
     * Recursive {@link Transformer} implementation that traverses the {@code "effects"} tree of an
     * enchantment JSON object, converting registered {@link #getEffectProperties()} nodes via
     * {@link ValueImporterRegistry} and {@link ValueExporterRegistry} while respecting type and structural exclusions.
     */
    public static class EffectsTransformer implements Transformer {
        @Override
        public void applyImport(JsonObject root, String targetNamespace, String enchantmentName) {
            if (!root.has("effects") || !root.get("effects").isJsonObject()) return;
            JsonObject effects = root.getAsJsonObject("effects");

            if (effects.has("enchantment_core:description_args")) {
                effects.remove("enchantment_core:description_args");
            }

            Map<String, Integer> keyFrequencyMap = new HashMap<>();
            scanAndImport(effects, targetNamespace, enchantmentName, keyFrequencyMap);
        }

        /**
         * Recursively traverses a JSON object subtree to import level-based value properties,
         * disambiguating duplicate property names within the same enchantment using a frequency counter.
         *
         * @param node            The current JSON object node being scanned.
         * @param targetNamespace The target mod namespace for generated configuration properties.
         * @param enchantmentName The enchantment path identifier used as the configuration group.
         * @param keyFrequencyMap A mutable map tracking how many times each sanitized key name has been encountered.
         */
        private void scanAndImport(JsonObject node, String targetNamespace, String enchantmentName, Map<String, Integer> keyFrequencyMap) {
            if (node.has("type") && getTypeExclusions().contains(node.get("type").getAsString())) {
                return;
            }

            List<String> keys = new ArrayList<>(node.keySet());
            for (String currentKey : keys) {
                if (getStructuralExclusions().contains(currentKey)) {
                    continue;
                }

                JsonElement element = node.get(currentKey);

                if (getEffectProperties().contains(currentKey)) {
                    JsonElement toImport = element;
                    if (element.isJsonPrimitive()) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("type", "minecraft:constant");
                        obj.add("value", element);
                        toImport = obj;
                    }

                    String cleanKey = currentKey.replace(":", "_").replace("-", "_");
                    int count = keyFrequencyMap.getOrDefault(cleanKey, 0) + 1;
                    keyFrequencyMap.put(cleanKey, count);
                    String propName = count == 1 ? cleanKey : cleanKey + "_" + count;

                    try {
                        JsonElement imported = ValueImporterRegistry.importLevelBasedValue(toImport, targetNamespace, enchantmentName, propName);
                        node.add(currentKey, imported);
                    } catch (Exception ignored) {}
                } else if (element.isJsonObject()) {
                    scanAndImport(element.getAsJsonObject(), targetNamespace, enchantmentName, keyFrequencyMap);
                } else if (element.isJsonArray()) {
                    for (JsonElement arrayElem : element.getAsJsonArray()) {
                        if (arrayElem.isJsonObject()) {
                            scanAndImport(arrayElem.getAsJsonObject(), targetNamespace, enchantmentName, keyFrequencyMap);
                        }
                    }
                }
            }
        }

        @Override
        public void applyExport(JsonObject root) {
            if (!root.has("effects") || !root.get("effects").isJsonObject()) return;
            JsonObject effects = root.getAsJsonObject("effects");

            if (effects.has("enchantment_core:description_args")) {
                effects.remove("enchantment_core:description_args");
            }

            scanAndExport(effects);
        }

        /**
         * Recursively traverses a JSON object subtree to export configurable level-based value properties
         * back into static representations.
         *
         * @param node The current JSON object node being scanned.
         */
        private void scanAndExport(JsonObject node) {
            if (node.has("type") && getTypeExclusions().contains(node.get("type").getAsString())) {
                return;
            }

            List<String> keys = new ArrayList<>(node.keySet());
            for (String currentKey : keys) {
                if (getStructuralExclusions().contains(currentKey)) {
                    continue;
                }

                JsonElement element = node.get(currentKey);

                if (getEffectProperties().contains(currentKey)) {
                    try {
                        JsonElement exported = ValueExporterRegistry.exportLevelBasedValue(element);
                        node.add(currentKey, exported);
                    } catch (Exception ignored) {}
                } else if (element.isJsonObject()) {
                    scanAndExport(element.getAsJsonObject());
                } else if (element.isJsonArray()) {
                    for (JsonElement arrayElem : element.getAsJsonArray()) {
                        if (arrayElem.isJsonObject()) {
                            scanAndExport(arrayElem.getAsJsonObject());
                        }
                    }
                }
            }
        }
    }
}