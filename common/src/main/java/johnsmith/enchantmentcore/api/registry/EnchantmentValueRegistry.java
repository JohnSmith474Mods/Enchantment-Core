package johnsmith.enchantmentcore.api.registry;

import com.mojang.serialization.MapCodec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigAwareValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurableClampedValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurableConstantValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurableDiminishingReturnsValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurableLevelsSquaredValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurableLinearValue;
import johnsmith.enchantmentcore.api.enchantment.value.configurable.ConfigurablePolynomialValue;
import johnsmith.enchantmentcore.api.enchantment.value.DiminishingReturnsValue;
import johnsmith.enchantmentcore.api.enchantment.value.NegateValue;
import johnsmith.enchantmentcore.api.enchantment.value.PointerValue;
import johnsmith.enchantmentcore.api.enchantment.value.PolynomialValue;
import johnsmith.enchantmentcore.api.enchantment.value.ProbabilisticValue;

import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Defines custom mathematical formulas for the vanilla {@link LevelBasedValue} ecosystem.
 * <p>
 * Stores codec definitions in memory without mutating {@code BuiltInRegistries} during static
 * initialization, allowing platform registrars to bind them during loader-specific events.
 */
public class EnchantmentValueRegistry {
    private static final Map<String, MapCodec<? extends LevelBasedValue>> VALUES = new LinkedHashMap<>();

    public static final MapCodec<ConfigurableConstantValue> CONFIGURABLE_CONSTANT =
            create(ConfigurableConstantValue.KEY, ConfigurableConstantValue.CODEC);
    public static final MapCodec<ConfigurableClampedValue> CONFIGURABLE_CLAMPED =
            create(ConfigurableClampedValue.KEY, ConfigurableClampedValue.CODEC);
    public static final MapCodec<ConfigurableDiminishingReturnsValue> CONFIGURABLE_DIMINISHING_RETURNS =
            create(ConfigurableDiminishingReturnsValue.KEY, ConfigurableDiminishingReturnsValue.CODEC);
    public static final MapCodec<ConfigurableLevelsSquaredValue> CONFIGURABLE_LEVELS_SQUARED =
            create(ConfigurableLevelsSquaredValue.KEY, ConfigurableLevelsSquaredValue.CODEC);
    public static final MapCodec<ConfigurableLinearValue> CONFIGURABLE_LINEAR =
            create(ConfigurableLinearValue.KEY, ConfigurableLinearValue.CODEC);
    public static final MapCodec<ConfigurablePolynomialValue> CONFIGURABLE_POLYNOMIAL =
            create(ConfigurablePolynomialValue.KEY, ConfigurablePolynomialValue.CODEC);
    public static final MapCodec<DiminishingReturnsValue> DIMINISHING_RETURNS =
            create(DiminishingReturnsValue.KEY, DiminishingReturnsValue.CODEC);
    public static final MapCodec<PolynomialValue> POLYNOMIAL =
            create(PolynomialValue.KEY, PolynomialValue.CODEC);
    public static final MapCodec<ConfigAwareValue> CONFIG_AWARE =
            create(ConfigAwareValue.KEY, ConfigAwareValue.CODEC);
    public static final MapCodec<ProbabilisticValue> PROBABILISTIC =
            create(ProbabilisticValue.KEY, ProbabilisticValue.CODEC);
    public static final MapCodec<NegateValue> NEGATIVE =
            create(NegateValue.KEY, NegateValue.CODEC);
    public static final MapCodec<PointerValue> POINTER =
            create(PointerValue.KEY, PointerValue.CODEC);

    private static <T extends LevelBasedValue> MapCodec<T> create(String name, MapCodec<T> codec) {
        VALUES.put(name, codec);
        return codec;
    }

    /**
     * Retrieves an unmodifiable view of all custom {@link LevelBasedValue} codecs keyed by path name.
     *
     * @return The map of level-based value codecs to register.
     */
    public static Map<String, MapCodec<? extends LevelBasedValue>> getValues() {
        return Collections.unmodifiableMap(VALUES);
    }

    /**
     * Triggers static initialization of level-based value codec definitions.
     */
    public static void initialize() {}
}