package johnsmith.enchantmentcore.api.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import java.util.function.Function;
import java.util.function.Supplier;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVisualEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVolumeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpellFieldShape;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Manages custom static registries required for the modular spell field system.
 * <p>
 * Initializes registry {@link ResourceKey}s, resolves registry suppliers from {@link BuiltInRegistries#REGISTRY},
 * and caches lazily initialized dispatch codecs for polymorphic serialization and deserialization of
 * {@link DistanceScalingFunction}s, {@link SpellFieldShape}s, and typed {@link SpellFieldEffect}s.
 */
public class EnchantmentCoreRegistries {
    // region Keys

    /** Registry key for {@link DistanceScalingFunction} codec registrations. */
    public static final ResourceKey<Registry<MapCodec<? extends DistanceScalingFunction>>> DISTANCE_SCALING_FUNCTION_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "distance_scaling_function"));

    /** Registry key for {@link SpellFieldShape} codec registrations. */
    public static final ResourceKey<Registry<MapCodec<? extends SpellFieldShape>>> SPELL_FIELD_SHAPE_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spell_field_shape"));

    /** Registry key for {@link SpellFieldEffect} codec registrations. */
    public static final ResourceKey<Registry<MapCodec<? extends SpellFieldEffect>>> SPELL_FIELD_EFFECT_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spell_field_effect"));
    // endregion

    // region Suppliers

    /** Supplier resolving the active {@link DistanceScalingFunction} registry from {@link BuiltInRegistries#REGISTRY}. */
    public static final Supplier<Registry<MapCodec<? extends DistanceScalingFunction>>> DISTANCE_SCALING_FUNCTIONS =
            createRegistrySupplier(DISTANCE_SCALING_FUNCTION_KEY);

    /** Supplier resolving the active {@link SpellFieldShape} registry from {@link BuiltInRegistries#REGISTRY}. */
    public static final Supplier<Registry<MapCodec<? extends SpellFieldShape>>> SPELL_FIELD_SHAPES =
            createRegistrySupplier(SPELL_FIELD_SHAPE_KEY);

    /** Supplier resolving the active {@link SpellFieldEffect} registry from {@link BuiltInRegistries#REGISTRY}. */
    public static final Supplier<Registry<MapCodec<? extends SpellFieldEffect>>> SPELL_FIELD_EFFECTS =
            createRegistrySupplier(SPELL_FIELD_EFFECT_KEY);

    /**
     * Creates a lazy supplier that queries {@link BuiltInRegistries#REGISTRY} for the specified registry key.
     *
     * @param key The target registry {@link ResourceKey}.
     * @param <T> The registry element type.
     * @return A supplier returning the resolved {@link Registry}.
     */
    @SuppressWarnings("unchecked")
    private static <T> Supplier<Registry<T>> createRegistrySupplier(ResourceKey<Registry<T>> key) {
        return () -> (Registry<T>) BuiltInRegistries.REGISTRY.getValue(key.location());
    }
    // endregion

    // region Cached Codecs
    private static Codec<DistanceScalingFunction> distanceScalingFunctionCodec = null;
    private static Codec<SpellFieldShape> spellFieldShapeCodec = null;
    private static Codec<SpellFieldEffect> baseSpellFieldEffectCodec = null;
    private static Codec<SpellFieldEntityEffect> spellFieldEntityEffectCodec = null;
    private static Codec<SpellFieldBlockEffect> spellFieldBlockEffectCodec = null;
    private static Codec<SpellFieldVolumeEffect> spellFieldVolumeEffectCodec = null;
    private static Codec<SpellFieldVisualEffect> spellFieldVisualEffectCodec = null;
    // endregion

    // region Dispatch Getters

    /**
     * Retrieves the polymorphic dispatch codec for {@link DistanceScalingFunction} instances.
     * <p>
     * Uses {@link Codec#lazyInitialized} so custom registries are not queried before
     * loader registry events populate {@link BuiltInRegistries#REGISTRY}.
     *
     * @return The cached {@link DistanceScalingFunction} dispatch codec.
     */
    public static Codec<DistanceScalingFunction> getDistanceScalingFunctionCodec() {
        if (distanceScalingFunctionCodec == null) {
            distanceScalingFunctionCodec = Codec.lazyInitialized(() ->
                    DISTANCE_SCALING_FUNCTIONS.get().byNameCodec()
                            .dispatch(DistanceScalingFunction::codec, Function.identity())
            );
        }
        return distanceScalingFunctionCodec;
    }

    /**
     * Retrieves the polymorphic dispatch codec for {@link SpellFieldShape} instances.
     * <p>
     * Uses {@link Codec#lazyInitialized} so custom registries are not queried before
     * loader registry events populate {@link BuiltInRegistries#REGISTRY}.
     *
     * @return The cached {@link SpellFieldShape} dispatch codec.
     */
    public static Codec<SpellFieldShape> getSpellFieldShapeCodec() {
        if (spellFieldShapeCodec == null) {
            spellFieldShapeCodec = Codec.lazyInitialized(() ->
                    SPELL_FIELD_SHAPES.get().byNameCodec()
                            .dispatch(SpellFieldShape::codec, Function.identity())
            );
        }
        return spellFieldShapeCodec;
    }

    /**
     * Retrieves the base polymorphic dispatch codec for all registered {@link SpellFieldEffect}s.
     *
     * @return The cached base {@link SpellFieldEffect} dispatch codec.
     */
    private static Codec<SpellFieldEffect> getBaseSpellFieldEffectCodec() {
        if (baseSpellFieldEffectCodec == null) {
            baseSpellFieldEffectCodec = Codec.lazyInitialized(() ->
                    SPELL_FIELD_EFFECTS.get().byNameCodec()
                            .dispatch(SpellFieldEffect::codec, Function.identity())
            );
        }
        return baseSpellFieldEffectCodec;
    }

    /**
     * Retrieves a strictly typed dispatch codec for entity-targeted spell field effects.
     * Rejects effects that do not implement the {@link SpellFieldEntityEffect} contract.
     *
     * @return The type-filtered {@link SpellFieldEntityEffect} codec.
     */
    public static Codec<SpellFieldEntityEffect> getSpellFieldEntityEffectCodec() {
        if (spellFieldEntityEffectCodec == null) {
            spellFieldEntityEffectCodec = getBaseSpellFieldEffectCodec().flatXmap(
                    effect -> effect instanceof SpellFieldEntityEffect e
                            ? DataResult.success(e)
                            : DataResult.error(() -> "Effect " + effect.getClass().getSimpleName() + " does not support Entity targeting."),
                    DataResult::success
            );
        }
        return spellFieldEntityEffectCodec;
    }

    /**
     * Retrieves a strictly typed dispatch codec for block-targeted spell field effects.
     * Rejects effects that do not implement the {@link SpellFieldBlockEffect} contract.
     *
     * @return The type-filtered {@link SpellFieldBlockEffect} codec.
     */
    public static Codec<SpellFieldBlockEffect> getSpellFieldBlockEffectCodec() {
        if (spellFieldBlockEffectCodec == null) {
            spellFieldBlockEffectCodec = getBaseSpellFieldEffectCodec().flatXmap(
                    effect -> effect instanceof SpellFieldBlockEffect e
                            ? DataResult.success(e)
                            : DataResult.error(() -> "Effect " + effect.getClass().getSimpleName() + " does not support Block targeting."),
                    DataResult::success
            );
        }
        return spellFieldBlockEffectCodec;
    }

    /**
     * Retrieves a strictly typed dispatch codec for volume-targeted spell field effects.
     * Rejects effects that do not implement the {@link SpellFieldVolumeEffect} contract.
     *
     * @return The type-filtered {@link SpellFieldVolumeEffect} codec.
     */
    public static Codec<SpellFieldVolumeEffect> getSpellFieldVolumeEffectCodec() {
        if (spellFieldVolumeEffectCodec == null) {
            spellFieldVolumeEffectCodec = getBaseSpellFieldEffectCodec().flatXmap(
                    effect -> effect instanceof SpellFieldVolumeEffect e
                            ? DataResult.success(e)
                            : DataResult.error(() -> "Effect " + effect.getClass().getSimpleName() + " does not support Volume targeting."),
                    DataResult::success
            );
        }
        return spellFieldVolumeEffectCodec;
    }

    /**
     * Retrieves a strictly typed dispatch codec for visual/particle spell field effects.
     * Rejects effects that do not implement the {@link SpellFieldVisualEffect} contract.
     *
     * @return The type-filtered {@link SpellFieldVisualEffect} codec.
     */
    public static Codec<SpellFieldVisualEffect> getSpellFieldVisualEffectCodec() {
        if (spellFieldVisualEffectCodec == null) {
            spellFieldVisualEffectCodec = getBaseSpellFieldEffectCodec().flatXmap(
                    effect -> effect instanceof SpellFieldVisualEffect e
                            ? DataResult.success(e)
                            : DataResult.error(() -> "Effect " + effect.getClass().getSimpleName() + " does not support Visual targeting."),
                    DataResult::success
            );
        }
        return spellFieldVisualEffectCodec;
    }
    // endregion

    /**
     * Initializes the custom registry keys and suppliers.
     */
    public static void initialize() {}
}