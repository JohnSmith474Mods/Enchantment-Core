package johnsmith.enchantmentcore.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import johnsmith.configoverhauled.api.data.ConfigDescription;

import johnsmith.enchantmentcore.api.config.ConfigReference;
import johnsmith.enchantmentcore.enchantment.value.configurable.ConfigurableConstantValue;
import johnsmith.enchantmentcore.enchantment.value.configurable.ConfigurableLevelsSquaredValue;
import johnsmith.enchantmentcore.enchantment.value.configurable.ConfigurableLinearValue;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Manages the interception of static LevelBasedValue JSON nodes and coordinates their
 * transformation into dynamic configuration-bound structures during the import command phase.
 */
public class ValueImporterRegistry {

    /**
     * Executes the structural mutation generating the config-bound wrapper block.
     */
    @FunctionalInterface
    public interface ValueImporter {
        /**
         * Mutates the source static element into a fully wrapped dynamic configuration object.
         *
         * @param original       The static JSON element targeted for transformation.
         * @param modId          The target namespace designated for the property manager.
         * @param group          The category grouping generated for the property manager.
         * @param propertyPrefix The specific property identifier segment.
         * @return The fully compiled JSON object wrapper.
         */
        JsonElement importFrom(JsonElement original, String modId, String group, String propertyPrefix);
    }

    /**
     * Executes the DFU state transfer populating a dynamically-typed class with standard vanilla configuration properties.
     */
    @FunctionalInterface
    public interface CodecImporter<S, T> {
        /**
         * Copies properties from a standard source to a new generic target.
         *
         * @param source         The static object implementation.
         * @param modId          The target namespace designated for the property manager.
         * @param group          The category grouping generated for the property manager.
         * @param propertyPrefix The specific property identifier segment.
         * @return The correctly populated target.
         */
        T importFrom(S source, String modId, String group, String propertyPrefix);
    }

    private static final Map<String, ValueImporter> REGISTRY = new HashMap<>();

    /**
     * Registers a structural wrapper generation routine for a specific JSON type definition.
     *
     * @param typeId   The exact JSON definition type string mapped (e.g. minecraft:linear).
     * @param importer The operational code executed upon parsing.
     */
    public static void register(String typeId, ValueImporter importer) {
        REGISTRY.put(typeId, importer);
    }

    /**
     * Retrieves the structural mapping procedure bound to a definition.
     *
     * @param typeId The specific identifier indicating the type.
     * @return The registered ValueImporter, or null if no mapping is found.
     */
    public static ValueImporter get(String typeId) {
        return REGISTRY.get(typeId);
    }

    /**
     * Binds a generic transformation procedure through the native DFU codec evaluation system.
     * Reads the structure via the standard source, transforms the class, and generates an evaluated wrapper format.
     *
     * @param sourceCodec The vanilla codec matching the original format.
     * @param targetCodec The dynamic codec matching the new target format.
     * @param targetType  The serialized type tag added to the new wrapper.
     * @param importer    The class transformation logic transferring data constraints.
     * @param <S>         The input data type class.
     * @param <T>         The resulting data type class.
     * @return A compiled generic ValueImporter.
     */
    public static <S, T> ValueImporter bindCodecs(MapCodec<S> sourceCodec, MapCodec<T> targetCodec, String targetType, CodecImporter<S, T> importer) {
        return (element, modId, group, prefix) -> {
            S source = sourceCodec.codec().parse(JsonOps.INSTANCE, element).getOrThrow(IllegalStateException::new);
            T target = importer.importFrom(source, modId, group, prefix);
            JsonObject output = targetCodec.codec().encodeStart(JsonOps.INSTANCE, target).getOrThrow(IllegalStateException::new).getAsJsonObject();
            output.addProperty("type", targetType);
            return output;
        };
    }

    private static ConfigReference createRef(String modId, String group, String property) {
        return new ConfigReference(new ConfigDescription(modId, "enchantment", group, property), Optional.empty());
    }

    /**
     * Utilities method that constructs the required layout definitions for the {@code "config"} json block.
     *
     * @param modId    The target namespace parsing configuration data.
     * @param group    The target structural group inside the configuration.
     * @param property The exact identifier mapping to the variable value.
     * @return The populated JSON object linking to the designated property.
     */
    public static JsonObject buildConfigObject(String modId, String group, String property) {
        JsonObject config = new JsonObject();
        config.addProperty("mod_id", modId);
        config.addProperty("category", "enchantment"); // Changed from "general"
        config.addProperty("group", group);
        config.addProperty("property", property);
        return config;
    }

    /**
     * Intercepts a JSON element designated for a specific target node and routes its evaluation through
     * the mapped wrapping algorithm. Defaults to a standard constant wrapper for missing tags or primitive integers.
     *
     * @param element  The source JSON value definition.
     * @param modId    The target namespace mapping to a defined configuration scope.
     * @param group    The structural container separating property variables.
     * @param property The generated unique variable identifier suffix.
     * @return The converted element format, or the original structure if unresolved.
     */
    public static JsonElement importLevelBasedValue(JsonElement element, String modId, String group, String property) {
        String type = "minecraft:constant";
        if (element.isJsonObject() && element.getAsJsonObject().has("type")) {
            type = element.getAsJsonObject().get("type").getAsString();
        } else if (element.isJsonPrimitive()) {
            type = "minecraft:constant";
        }

        if (!type.contains(":")) {
            type = "minecraft:" + type;
        }

        ValueImporter importer = get(type);
        if (importer != null) {
            return importer.importFrom(element, modId, group, property);
        }

        return element;
    }

    static {
        register("minecraft:constant", bindCodecs(
                LevelBasedValue.Constant.TYPED_CODEC,
                ConfigurableConstantValue.CODEC,
                "enchantment_core:configurable_constant",
                (source, modId, group, prefix) -> new ConfigurableConstantValue(
                        createRef(modId, group, prefix), source.value(), -Float.MAX_VALUE, Float.MAX_VALUE)
        ));

        register("minecraft:linear", bindCodecs(
                LevelBasedValue.Linear.CODEC,
                ConfigurableLinearValue.CODEC,
                "enchantment_core:configurable_linear",
                (source, modId, group, prefix) -> new ConfigurableLinearValue(
                        createRef(modId, group, prefix + "_base"), source.base(), -Float.MAX_VALUE, Float.MAX_VALUE,
                        createRef(modId, group, prefix + "_per_level"), source.perLevelAboveFirst(), -Float.MAX_VALUE, Float.MAX_VALUE)
        ));

        register("minecraft:levels_squared", bindCodecs(
                LevelBasedValue.LevelsSquared.CODEC,
                ConfigurableLevelsSquaredValue.CODEC,
                "enchantment_core:configurable_levels_squared",
                (source, modId, group, prefix) -> new ConfigurableLevelsSquaredValue(
                        createRef(modId, group, prefix), source.added(), -Float.MAX_VALUE, Float.MAX_VALUE)
        ));

        register("minecraft:clamped", (element, modId, group, property) -> {
            JsonObject obj = element.getAsJsonObject();
            JsonObject out = new JsonObject();
            out.addProperty("type", "enchantment_core:configurable_clamped");
            if (obj.has("value")) out.add("value", importLevelBasedValue(obj.get("value"), modId, group, property + "_value"));
            out.add("min_config", buildConfigObject(modId, group, property + "_min"));
            out.addProperty("min_default", obj.has("min") ? obj.get("min").getAsFloat() : 0.0f);
            out.add("max_config", buildConfigObject(modId, group, property + "_max"));
            out.addProperty("max_default", obj.has("max") ? obj.get("max").getAsFloat() : 0.0f);
            return out;
        });

        register("minecraft:fraction", (element, modId, group, property) -> {
            JsonObject obj = element.getAsJsonObject().deepCopy();
            if (obj.has("numerator")) obj.add("numerator", importLevelBasedValue(obj.get("numerator"), modId, group, property + "_numerator"));
            if (obj.has("denominator")) obj.add("denominator", importLevelBasedValue(obj.get("denominator"), modId, group, property + "_denominator"));
            return obj;
        });

        register("minecraft:lookup", (element, modId, group, property) -> {
            JsonObject obj = element.getAsJsonObject().deepCopy();
            if (obj.has("fallback")) obj.add("fallback", importLevelBasedValue(obj.get("fallback"), modId, group, property + "_fallback"));
            return obj;
        });

        register("enchantment_core:negate", (element, modId, group, property) -> {
            JsonObject obj = element.getAsJsonObject().deepCopy();
            if (obj.has("value")) obj.add("value", importLevelBasedValue(obj.get("value"), modId, group, property + "_value"));
            return obj;
        });

        register("enchantment_core:probabilistic", (element, modId, group, property) -> {
            JsonObject obj = element.getAsJsonObject().deepCopy();
            if (obj.has("chance")) obj.add("chance", importLevelBasedValue(obj.get("chance"), modId, group, property + "_chance"));
            return obj;
        });
    }
}