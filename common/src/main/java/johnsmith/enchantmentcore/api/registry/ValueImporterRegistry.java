package johnsmith.enchantmentcore.api.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.enchantmentcore.api.config.ConfigReference;
import johnsmith.enchantmentcore.api.enchantment.value.DiminishingReturnsValue;
import johnsmith.enchantmentcore.api.enchantment.value.PolynomialValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurableConstantValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurableDiminishingReturnsValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurableLevelsSquaredValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurableLinearValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurablePolynomialValue;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Manages the interception of static {@link LevelBasedValue} JSON nodes and coordinates their
 * transformation into dynamic configuration-bound structures during the {@code /enchantment_core import} phase.
 */
public class ValueImporterRegistry {

    /**
     * Functional contract for mutating a static {@link LevelBasedValue} JSON element into a
     * configuration-backed JSON wrapper.
     */
    @FunctionalInterface
    public interface ValueImporter {
        /**
         * Transforms the static JSON element into a dynamic configuration-bound JSON object.
         *
         * @param original       The static JSON element targeted for transformation.
         * @param modId          The target mod namespace designated for the configuration manager.
         * @param group          The configuration group identifier (typically the enchantment's path name).
         * @param propertyPrefix The base property identifier segment for generated configuration keys.
         * @return The transformed JSON element.
         */
        JsonElement importFrom(JsonElement original, String modId, String group, String propertyPrefix);
    }

    /**
     * Functional contract for converting a decoded static {@link LevelBasedValue} instance into its
     * corresponding configurable {@link LevelBasedValue} counterpart.
     *
     * @param <S> The static source value type.
     * @param <T> The configurable target value type.
     */
    @FunctionalInterface
    public interface CodecImporter<S, T> {
        /**
         * Constructs a configurable value instance from a decoded static source instance.
         *
         * @param source         The decoded static {@link LevelBasedValue} instance.
         * @param modId          The target mod namespace designated for the configuration manager.
         * @param group          The configuration group identifier.
         * @param propertyPrefix The base property identifier segment.
         * @return The populated configurable value instance.
         */
        T importFrom(S source, String modId, String group, String propertyPrefix);
    }

    /** Internal map binding static value type identifiers to their {@link ValueImporter} delegates. */
    private static final Map<String, ValueImporter> REGISTRY = new HashMap<>();

    /**
     * Registers a {@link ValueImporter} routine for a specific static {@link LevelBasedValue} type identifier.
     *
     * @param typeId   The resource location string of the static value type (e.g., {@code "minecraft:linear"}).
     * @param importer The {@link ValueImporter} executed when transforming matching nodes.
     */
    public static void register(String typeId, ValueImporter importer) {
        REGISTRY.put(typeId, importer);
    }

    /**
     * Retrieves the {@link ValueImporter} bound to the specified static type identifier.
     *
     * @param typeId The resource location string of the static value type.
     * @return The registered {@link ValueImporter}, or {@code null} if no mapping exists.
     */
    public static ValueImporter get(String typeId) {
        return REGISTRY.get(typeId);
    }

    /**
     * Constructs a codec-driven {@link ValueImporter} that decodes the static JSON element via
     * {@code sourceCodec}, transforms it with {@code importer}, and encodes the resulting configurable
     * instance via {@code targetCodec}.
     *
     * @param sourceCodec The {@link MapCodec} matching the original static format.
     * @param targetCodec The {@link MapCodec} matching the new configurable target format.
     * @param targetType  The {@code "type"} identifier string added to the encoded JSON wrapper.
     * @param importer    The transformation logic mapping the static object to its configurable counterpart.
     * @param <S>         The static value class.
     * @param <T>         The configurable output class.
     * @return A compiled {@link ValueImporter}.
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
     * Utility method that constructs a serialized {@link ConfigDescription} JSON object under the
     * {@code "enchantment"} category.
     *
     * @param modId    The target mod namespace.
     * @param group    The target configuration group inside the {@code "enchantment"} category.
     * @param property The specific property identifier key.
     * @return A populated {@link JsonObject} representing the configuration target.
     */
    public static JsonObject buildConfigObject(String modId, String group, String property) {
        JsonObject config = new JsonObject();
        config.addProperty("mod_id", modId);
        config.addProperty("category", "enchantment");
        config.addProperty("group", group);
        config.addProperty("property", property);
        return config;
    }

    /**
     * Intercepts a JSON element representing a {@link LevelBasedValue} and routes it through the
     * appropriate {@link ValueImporter} based on its {@code "type"} field (defaulting to
     * {@code "minecraft:constant"} for primitives or objects lacking a type tag).
     *
     * @param element  The source JSON value definition.
     * @param modId    The target mod namespace for generated configuration properties.
     * @param group    The configuration group name.
     * @param property The unique property identifier prefix.
     * @return The transformed configurable JSON element, or the original element if no importer matches.
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

        register("enchantment_core:pointer", (element, modId, group, property) -> {
            JsonObject obj = element.getAsJsonObject().deepCopy();
            if (obj.has("value")) obj.add("value", importLevelBasedValue(obj.get("value"), modId, group, property + "_value"));
            return obj;
        });

        register("enchantment_core:diminishing_returns", bindCodecs(
                DiminishingReturnsValue.CODEC,
                ConfigurableDiminishingReturnsValue.CODEC,
                "enchantment_core:configurable_diminishing_returns",
                (source, modId, group, prefix) -> new ConfigurableDiminishingReturnsValue(
                        createRef(modId, group, prefix + "_base"), source.base(),
                        createRef(modId, group, prefix + "_decrement"), source.decrement(),
                        createRef(modId, group, prefix + "_minimum"), source.minimum()
                )
        ));

        register("enchantment_core:polynomial", bindCodecs(
                PolynomialValue.CODEC,
                ConfigurablePolynomialValue.CODEC,
                "enchantment_core:configurable_polynomial",
                (source, modId, group, prefix) -> new ConfigurablePolynomialValue(
                        createRef(modId, group, prefix + "_scale"), source.scale(),
                        createRef(modId, group, prefix + "_power"), source.power(),
                        createRef(modId, group, prefix + "_offset"), source.offset(),
                        createRef(modId, group, prefix + "_level_offset"), source.levelOffset()
                )
        ));
    }
}