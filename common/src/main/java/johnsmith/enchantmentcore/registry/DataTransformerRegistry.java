package johnsmith.enchantmentcore.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import johnsmith.enchantmentcore.config.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Centralized registry managing the bidirectional transformation of enchantment JSON data.
 * Intercepts static primitive values during import and wraps them in dynamic configuration objects.
 * Strips dynamic configuration wrappers during export to restore standard JSON architecture.
 */
public class DataTransformerRegistry {

    public static final String KEY_MAX_LEVEL = "max_level";
    public static final String KEY_WEIGHT = "weight";
    public static final String KEY_ANVIL_COST = "anvil_cost";
    public static final String KEY_SUPPORTED_ITEMS = "supported_items";
    public static final String KEY_PRIMARY_ITEMS = "primary_items";
    public static final String KEY_SLOTS = "slots";
    public static final String KEY_MIN_COST = "min_cost";
    public static final String KEY_MAX_COST = "max_cost";

    /**
     * Defines the contract for executing targeted data transformations on a JSON node.
     */
    public interface Transformer {
        /**
         * Executes the import transformation logic.
         *
         * @param root            The JSON object being mutated.
         * @param targetNamespace The namespace assigned to the imported configuration.
         * @param enchantmentName The base identifier of the target enchantment.
         */
        void applyImport(JsonObject root, String targetNamespace, String enchantmentName);

        /**
         * Executes the export transformation logic.
         *
         * @param root The JSON object being mutated.
         */
        void applyExport(JsonObject root);
    }

    private static final List<Transformer> TRANSFORMERS = new ArrayList<>();
    private static final Set<String> EFFECT_PROPERTIES = new HashSet<>();

    private static final Set<String> TYPE_EXCLUSIONS = new HashSet<>(Set.of(
            "minecraft:play_sound",
            "minecraft:spawn_particles"
    ));

    private static final Set<String> STRUCTURAL_EXCLUSIONS = new HashSet<>(Set.of(
            "requirements"
    ));

    /**
     * Registers a structural transformer to the execution pipeline.
     *
     * @param transformer The transformer implementation.
     */
    public static void registerTransformer(Transformer transformer) {
        TRANSFORMERS.add(transformer);
    }

    /**
     * Registers a JSON key identifying a node that holds LevelBasedValue data.
     *
     * @param propertyKey The exact string identifier of the JSON key.
     */
    public static void registerEffectProperty(String propertyKey) {
        EFFECT_PROPERTIES.add(propertyKey);
    }

    /**
     * Registers an effect type identifier to bypass transformation.
     * Prevents DataFixerUpper crashes when vanilla effects utilize restricted primitives (e.g., FloatProvider).
     *
     * @param typeId The resource location string of the effect type.
     */
    public static void registerTypeExclusion(String typeId) {
        TYPE_EXCLUSIONS.add(typeId);
    }

    /**
     * Registers multiple effect type identifiers to bypass transformation.
     *
     * @param typeIds A collection of resource location strings.
     */
    public static void registerTypeExclusions(Collection<String> typeIds) {
        TYPE_EXCLUSIONS.addAll(typeIds);
    }

    /**
     * Registers a JSON key identifying a structural node to bypass during recursive traversal.
     * Isolates transformation logic from independent logic trees containing identical property names.
     *
     * @param nodeKey The exact string identifier of the structural node.
     */
    public static void registerStructuralExclusion(String nodeKey) {
        STRUCTURAL_EXCLUSIONS.add(nodeKey);
    }

    /**
     * Registers multiple JSON keys identifying structural nodes to bypass during recursive traversal.
     *
     * @param nodeKeys A collection of string identifiers.
     */
    public static void registerStructuralExclusions(Collection<String> nodeKeys) {
        STRUCTURAL_EXCLUSIONS.addAll(nodeKeys);
    }

    /**
     * Retrieves the list of active transformers.
     *
     * @return The transformer list.
     */
    public static List<Transformer> getTransformers() {
        return TRANSFORMERS;
    }

    /**
     * Retrieves the set of registered JSON keys targeted for transformation.
     *
     * @return The target key set.
     */
    public static Set<String> getEffectProperties() {
        return EFFECT_PROPERTIES;
    }

    /**
     * Retrieves the active set of effect type exclusions, merging programmatic registrations with user configuration.
     *
     * @return The consolidated exclusion set.
     */
    public static Set<String> getTypeExclusions() {
        Set<String> exclusions = new HashSet<>(TYPE_EXCLUSIONS);
        List<String> userExclusions = Config.USER_TYPE_EXCLUSIONS.get();
        if (userExclusions != null) {
            exclusions.addAll(userExclusions);
        }
        return exclusions;
    }

    /**
     * Retrieves the active set of structural node exclusions, merging programmatic registrations with user configuration.
     *
     * @return The consolidated exclusion set.
     */
    public static Set<String> getStructuralExclusions() {
        Set<String> exclusions = new HashSet<>(STRUCTURAL_EXCLUSIONS);
        List<String> userExclusions = Config.USER_STRUCTURAL_EXCLUSIONS.get();
        if (userExclusions != null) {
            exclusions.addAll(userExclusions);
        }
        return exclusions;
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
     * Transformer implementation handling standard single-node replacements.
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
     * Transformer implementation handling composite enchantment cost structures.
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
     * Transformer implementation executing recursive evaluation of nested effect arrays.
     */
    public static class EffectsTransformer implements Transformer {
        @Override
        public void applyImport(JsonObject root, String targetNamespace, String enchantmentName) {
            if (!root.has("effects") || !root.get("effects").isJsonObject()) return;
            JsonObject effects = root.getAsJsonObject("effects");
            Map<String, Integer> keyFrequencyMap = new HashMap<>();
            scanAndImport(effects, targetNamespace, enchantmentName, keyFrequencyMap);
        }

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
            scanAndExport(effects);
        }

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