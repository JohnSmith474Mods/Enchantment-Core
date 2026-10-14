package johnsmith.enchantmentcore.api.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

import java.util.HashMap;
import java.util.Map;

import johnsmith.enchantmentcore.api.enchantment.value.DiminishingReturnsValue;
import johnsmith.enchantmentcore.api.enchantment.value.PolynomialValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurableConstantValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurableDiminishingReturnsValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurableLevelsSquaredValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurableLinearValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurablePolynomialValue;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Manages the reverse transformation of dynamic, configuration-backed {@link LevelBasedValue} JSON objects
 * back into static vanilla or non-configurable representations during the {@code /enchantment_core export} phase.
 */
public class ValueExporterRegistry {

    /**
     * Functional contract for collapsing a configurable JSON object into its static JSON representation.
     */
    @FunctionalInterface
    public interface ValueExporter {
        /**
         * Reverts the configurable object wrapper back into a static JSON object or primitive.
         *
         * @param originalConfigurableObject The JSON object defining the dynamic configuration data.
         * @return The stripped JSON element representing a static value.
         */
        JsonElement exportFrom(JsonObject originalConfigurableObject);
    }

    /**
     * Functional contract for reconstructing a static {@link LevelBasedValue} instance from a parsed
     * configurable instance during DFU codec evaluation.
     *
     * @param <S> The source configurable value type.
     * @param <T> The target static value type.
     */
    @FunctionalInterface
    public interface CodecExporter<S, T> {
        /**
         * Converts a parsed configurable value instance into its static equivalent using its default values.
         *
         * @param source The parsed configurable source instance.
         * @return The reconstructed static target instance.
         */
        T exportFrom(S source);
    }

    /** Internal map binding configurable value type identifiers to their {@link ValueExporter} delegates. */
    private static final Map<String, ValueExporter> REGISTRY = new HashMap<>();

    /**
     * Registers a {@link ValueExporter} for a specific configurable {@link LevelBasedValue} type identifier.
     *
     * @param configurableTypeId The resource location string of the configurable value type (e.g., {@code "enchantment_core:configurable_linear"}).
     * @param exporter           The {@link ValueExporter} responsible for unwrapping that type.
     */
    public static void register(String configurableTypeId, ValueExporter exporter) {
        REGISTRY.put(configurableTypeId, exporter);
    }

    /**
     * Retrieves the {@link ValueExporter} registered for the specified configurable type identifier.
     *
     * @param configurableTypeId The resource location string of the configurable value type.
     * @return The registered {@link ValueExporter}, or {@code null} if none is bound.
     */
    public static ValueExporter get(String configurableTypeId) {
        return REGISTRY.get(configurableTypeId);
    }

    /**
     * Constructs a codec-driven {@link ValueExporter} that decodes the configurable JSON object via
     * {@code sourceCodec}, transforms it with {@code exporter}, and encodes the resulting static instance
     * via {@code targetCodec}.
     *
     * @param sourceCodec The {@link MapCodec} for the configurable source structure.
     * @param targetCodec The {@link MapCodec} for the resulting static structure.
     * @param targetType  The {@code "type"} identifier string written to the exported JSON object.
     * @param exporter    The transformation function mapping the configurable instance to a static instance.
     * @param <S>         The configurable source class.
     * @param <T>         The static target class.
     * @return A compiled {@link ValueExporter}.
     */
    public static <S, T> ValueExporter bindCodecs(MapCodec<S> sourceCodec, MapCodec<T> targetCodec, String targetType, CodecExporter<S, T> exporter) {
        return element -> {
            S source = sourceCodec.codec().parse(JsonOps.INSTANCE, element).getOrThrow(IllegalStateException::new);
            T target = exporter.exportFrom(source);
            JsonObject output = targetCodec.codec().encodeStart(JsonOps.INSTANCE, target).getOrThrow(IllegalStateException::new).getAsJsonObject();
            output.addProperty("type", targetType);
            return output;
        };
    }

    /**
     * Evaluates a JSON element representing a {@link LevelBasedValue} and recursively unwraps any
     * configurable wrappers into static definitions.
     *
     * @param element The JSON element to export.
     * @return The unwrapped static JSON element, or the original element if not a recognized configurable type.
     */
    public static JsonElement exportLevelBasedValue(JsonElement element) {
        if (!element.isJsonObject()) return element;
        JsonObject obj = element.getAsJsonObject();

        String type = "minecraft:constant";
        if (obj.has("type")) {
            type = obj.get("type").getAsString();
        }

        ValueExporter exporter = get(type);
        if (exporter != null) {
            return exporter.exportFrom(obj);
        }

        if (type.equals("minecraft:fraction") || type.equals("fraction")) {
            if (obj.has("numerator")) obj.add("numerator", exportLevelBasedValue(obj.get("numerator")));
            if (obj.has("denominator")) obj.add("denominator", exportLevelBasedValue(obj.get("denominator")));
        } else if (type.equals("minecraft:lookup") || type.equals("lookup")) {
            if (obj.has("fallback")) obj.add("fallback", exportLevelBasedValue(obj.get("fallback")));
        }

        return obj;
    }

    static {
        register("enchantment_core:configurable_constant", bindCodecs(
                ConfigurableConstantValue.CODEC,
                LevelBasedValue.Constant.TYPED_CODEC,
                "minecraft:constant",
                source -> new LevelBasedValue.Constant(source.defaultValue())
        ));

        register("enchantment_core:configurable_linear", bindCodecs(
                ConfigurableLinearValue.CODEC,
                LevelBasedValue.Linear.CODEC,
                "minecraft:linear",
                source -> new LevelBasedValue.Linear(source.baseDefault(), source.perLevelDefault())
        ));

        register("enchantment_core:configurable_levels_squared", bindCodecs(
                ConfigurableLevelsSquaredValue.CODEC,
                LevelBasedValue.LevelsSquared.CODEC,
                "minecraft:levels_squared",
                source -> new LevelBasedValue.LevelsSquared(source.defaultValue())
        ));

        register("enchantment_core:configurable_diminishing_returns", bindCodecs(
                ConfigurableDiminishingReturnsValue.CODEC,
                DiminishingReturnsValue.CODEC,
                "enchantment_core:diminishing_returns",
                source -> new DiminishingReturnsValue(source.baseDefault(), source.decrementDefault(), source.minimumDefault())
        ));

        register("enchantment_core:configurable_polynomial", bindCodecs(
                ConfigurablePolynomialValue.CODEC,
                PolynomialValue.CODEC,
                "enchantment_core:polynomial",
                source -> new PolynomialValue(source.scaleDefault(), source.powerDefault(), source.offsetDefault(), source.levelOffsetDefault())
        ));

        register("enchantment_core:configurable_clamped", obj -> {
            JsonObject out = new JsonObject();
            out.addProperty("type", "minecraft:clamped");
            if (obj.has("value")) out.add("value", exportLevelBasedValue(obj.get("value")));
            out.addProperty("min", obj.has("min_default") ? obj.get("min_default").getAsFloat() : 0.0f);
            out.addProperty("max", obj.has("max_default") ? obj.get("max_default").getAsFloat() : 0.0f);
            return out;
        });

        register("enchantment_core:negate", obj -> {
            JsonObject out = obj.deepCopy();
            if (out.has("value")) out.add("value", exportLevelBasedValue(out.get("value")));
            return out;
        });

        register("enchantment_core:probabilistic", obj -> {
            JsonObject out = obj.deepCopy();
            if (out.has("chance")) out.add("chance", exportLevelBasedValue(out.get("chance")));
            return out;
        });

        register("enchantment_core:pointer", obj -> {
            JsonObject out = obj.deepCopy();
            if (out.has("value")) out.add("value", exportLevelBasedValue(out.get("value")));
            return out;
        });
    }
}