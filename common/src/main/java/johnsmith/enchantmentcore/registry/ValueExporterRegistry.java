package johnsmith.enchantmentcore.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

import java.util.HashMap;
import java.util.Map;

import johnsmith.enchantmentcore.enchantment.value.DiminishingReturnsValue;
import johnsmith.enchantmentcore.enchantment.value.PolynomialValue;
import johnsmith.enchantmentcore.enchantment.value.configurable.*;
import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Manages the reverse transformation of dynamic, configurable LevelBasedValue objects back into
 * static vanilla JSON representations during the data pack export sequence.
 */
public class ValueExporterRegistry {

    /**
     * Executes the structural collapse of a specific configurable JSON object.
     */
    @FunctionalInterface
    public interface ValueExporter {
        /**
         * Reverts the configurable object wrapper back into a static primitive or standard schema.
         *
         * @param originalConfigurableObject The JSON object defining the dynamic configuration data.
         * @return The stripped JSON element representing a static value.
         */
        JsonElement exportFrom(JsonObject originalConfigurableObject);
    }

    /**
     * Reconstructs static instances from dynamically sourced instances during the DFU evaluation stage.
     */
    @FunctionalInterface
    public interface CodecExporter<S, T> {
        /**
         * Creates a vanilla equivalent for a configured element.
         *
         * @param source The parsed source instance.
         * @return The reconstructed target instance.
         */
        T exportFrom(S source);
    }

    private static final Map<String, ValueExporter> REGISTRY = new HashMap<>();

    /**
     * Registers a structural un-wrapping procedure.
     *
     * @param configurableTypeId The identifier for the custom configurable object map.
     * @param exporter           The delegated execution block defining the un-wrapping logic.
     */
    public static void register(String configurableTypeId, ValueExporter exporter) {
        REGISTRY.put(configurableTypeId, exporter);
    }

    /**
     * Retrieves the structural un-wrapping procedure for the specified custom format.
     *
     * @param configurableTypeId The identifier defining the map format.
     * @return The assigned ValueExporter, or null if unassigned.
     */
    public static ValueExporter get(String configurableTypeId) {
        return REGISTRY.get(configurableTypeId);
    }

    /**
     * Constructs a generic exporter by running the source JSON block through the target Codec
     * and reconstructing the stripped structure directly.
     *
     * @param sourceCodec The codec for the current dynamic element structure.
     * @param targetCodec The codec for the resulting vanilla element structure.
     * @param targetType  The JSON identifier indicating the new type.
     * @param exporter    The transformation lambda mapping properties between elements.
     * @param <S>         The input data type class.
     * @param <T>         The resulting data type class.
     * @return An operational generic exporter.
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
     * Evaluates a targeted JSON element and attempts to un-wrap its contents into a standard static format.
     * Performs recursive extraction on fractional elements.
     *
     * @param element The targeted JSON element.
     * @return The successfully stripped element, or the original element if not matched.
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
        } else if (type.equals("enchantment_core:negate")) {
            if (obj.has("value")) obj.add("value", exportLevelBasedValue(obj.get("value")));
        } else if (type.equals("enchantment_core:probabilistic")) {
            if (obj.has("chance")) obj.add("chance", exportLevelBasedValue(obj.get("chance")));
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
    }
}