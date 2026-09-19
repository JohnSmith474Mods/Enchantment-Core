package johnsmith.enchantmentcore.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import java.util.function.Function;
import java.util.function.Supplier;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVisualEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVolumeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpellFieldShape;
import johnsmith.enchantmentcore.platform.Services;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Manages custom dynamic registries required for the modular spell field system.
 * Initializes registry keys, registers suppliers for custom registries, and caches dispatch codecs
 * for polymorphic serialization and deserialization.
 */
public class EnchantmentCoreRegistries {

    // region Keys
    public static final ResourceKey<Registry<MapCodec<? extends DistanceScalingFunction>>> DISTANCE_SCALING_FUNCTION_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "distance_scaling_function"));

    public static final ResourceKey<Registry<MapCodec<? extends SpellFieldShape>>> SPELL_FIELD_SHAPE_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spell_field_shape"));

    public static final ResourceKey<Registry<MapCodec<? extends SpellFieldEffect>>> SPELL_FIELD_EFFECT_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spell_field_effect"));// endregion

    // region Suppliers
    public static final Supplier<Registry<MapCodec<? extends DistanceScalingFunction>>> DISTANCE_SCALING_FUNCTIONS =
            Services.REGISTRIES.createCustomRegistry(DISTANCE_SCALING_FUNCTION_KEY);

    public static final Supplier<Registry<MapCodec<? extends SpellFieldShape>>> SPELL_FIELD_SHAPES =
            Services.REGISTRIES.createCustomRegistry(SPELL_FIELD_SHAPE_KEY);

    public static final Supplier<Registry<MapCodec<? extends SpellFieldEffect>>> SPELL_FIELD_EFFECTS =
            Services.REGISTRIES.createCustomRegistry(SPELL_FIELD_EFFECT_KEY); // endregion

    // region Cached Codecs
    private static Codec<DistanceScalingFunction> distanceScalingFunctionCodec = null;
    private static Codec<SpellFieldShape> spellFieldShapeCodec = null;
    private static Codec<SpellFieldEffect> baseSpellFieldEffectCodec = null;

    private static Codec<SpellFieldEntityEffect> spellFieldEntityEffectCodec = null;
    private static Codec<SpellFieldBlockEffect> spellFieldBlockEffectCodec = null;
    private static Codec<SpellFieldVolumeEffect> spellFieldVolumeEffectCodec = null;
    private static Codec<SpellFieldVisualEffect> spellFieldVisualEffectCodec = null; // endregion

    // region Dispatch Getters
    /**
     * Retrieves the dispatch codec for distance scaling functions.
     * Allows polymorphic JSON resolution for mathematical falloff algorithms.
     *
     * @return The cached distance scaling function codec.
     */
    public static Codec<DistanceScalingFunction> getDistanceScalingFunctionCodec() {
        if (distanceScalingFunctionCodec == null) {
            distanceScalingFunctionCodec = DISTANCE_SCALING_FUNCTIONS.get().byNameCodec()
                    .dispatch(DistanceScalingFunction::codec, Function.identity());
        }
        return distanceScalingFunctionCodec;
    }

    /**
     * Retrieves the dispatch codec for spell field shapes.
     * Allows polymorphic JSON resolution for geometric bounding structures.
     *
     * @return The cached spell field shape codec.
     */
    public static Codec<SpellFieldShape> getSpellFieldShapeCodec() {
        if (spellFieldShapeCodec == null) {
            spellFieldShapeCodec = SPELL_FIELD_SHAPES.get().byNameCodec()
                    .dispatch(SpellFieldShape::codec, Function.identity());
        }
        return spellFieldShapeCodec;
    }

    private static Codec<SpellFieldEffect> getBaseSpellFieldEffectCodec() {
        if (baseSpellFieldEffectCodec == null) {
            baseSpellFieldEffectCodec = SPELL_FIELD_EFFECTS.get().byNameCodec()
                    .dispatch(SpellFieldEffect::codec, Function.identity());
        }
        return baseSpellFieldEffectCodec;
    }

    /**
     * Retrieves a strictly typed dispatch codec for entity-targeted spell field effects.
     * Rejects generic effects that do not implement the {@link SpellFieldEntityEffect} contract.
     *
     * @return The filtered entity effect codec.
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
     * Rejects generic effects that do not implement the {@link SpellFieldBlockEffect} contract.
     *
     * @return The filtered block effect codec.
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
     * Rejects generic effects that do not implement the {@link SpellFieldVolumeEffect} contract.
     *
     * @return The filtered volume effect codec.
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
     * Retrieves a strictly typed dispatch codec for visually-targeted spell field effects.
     * Rejects generic effects that do not implement the {@link SpellFieldVisualEffect} contract.
     *
     * @return The filtered visual effect codec.
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
    } // endregion

    /**
     * Initializes the custom registries. Must be called during the mod's setup phase.
     */
    public static void initialize() {}
}