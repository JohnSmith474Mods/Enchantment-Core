package johnsmith.enchantmentcore.registry;

import com.mojang.serialization.MapCodec;

import java.util.function.BiConsumer;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpellFieldShape;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEffect;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.audio.PlaySoundBlockEffect;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.audio.PlaySoundEntityEffect;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.audio.PlaySoundVolumeEffect;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.block.*;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.entity.*;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.visual.AmbientFieldVisualEffect;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.visual.BurstBlockVisualEffect;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.visual.BurstEntityVisualEffect;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.visual.VectorFieldVisualEffect;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.volume.ExplodeEffect;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.volume.LightningStrikeEffect;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.volume.SurfaceHazardEffect;
import johnsmith.enchantmentcore.enchantment.spellfield.math.function.*;
import johnsmith.enchantmentcore.enchantment.spellfield.shape.CubeShape;
import johnsmith.enchantmentcore.enchantment.spellfield.shape.CuboidShape;
import johnsmith.enchantmentcore.enchantment.spellfield.shape.DeepCuboidShape;
import johnsmith.enchantmentcore.enchantment.spellfield.shape.WideCuboidShape;

import net.minecraft.resources.ResourceLocation;

/**
 * A centralized registration utility specifically for binding spell field implementations to their identifier codecs.
 * Evaluates keys required by the DataTransformerRegistry to guarantee dynamically generated configurations map correctly.
 */
public class SpellFieldRegistrar {

    /**
     * Feeds the list of valid distance scaling function codecs into the provided registrar block.
     *
     * @param registrar A consumer capturing the evaluated resource location and its associated scaling map codec.
     */
    public static void registerFunctions(BiConsumer<ResourceLocation, MapCodec<? extends DistanceScalingFunction>> registrar) {
        registrar.accept(id(ConstantScalingFunction.KEY), ConstantScalingFunction.CODEC);
        registrar.accept(id(LinearScalingFunction.KEY), LinearScalingFunction.CODEC);
        registrar.accept(id(PolynomialScalingFunction.KEY), PolynomialScalingFunction.CODEC);
        registrar.accept(id(ExponentialScalingFunction.KEY), ExponentialScalingFunction.CODEC);
        registrar.accept(id(LogarithmicScalingFunction.KEY), LogarithmicScalingFunction.CODEC);
        registrar.accept(id(SigmoidScalingFunction.KEY), SigmoidScalingFunction.CODEC);
    }

    /**
     * Feeds the list of valid geometric shape codecs into the provided registrar block.
     * Dispatches any dynamic keys associated with the shapes to the DataTransformerRegistry.
     *
     * @param registrar A consumer capturing the evaluated resource location and its associated shape map codec.
     */
    public static void registerShapes(BiConsumer<ResourceLocation, MapCodec<? extends SpellFieldShape>> registrar) {
        registerShape(registrar, CubeShape.KEY, CubeShape.CODEC, CubeShape.KEY_PROVIDER);
        registerShape(registrar, CuboidShape.KEY, CuboidShape.CODEC, CuboidShape.KEY_PROVIDER);
        registerShape(registrar, DeepCuboidShape.KEY, DeepCuboidShape.CODEC, DeepCuboidShape.KEY_PROVIDER);
        registerShape(registrar, WideCuboidShape.KEY, WideCuboidShape.CODEC, WideCuboidShape.KEY_PROVIDER);
    }

    /**
     * Feeds the list of valid spell field operational effects into the provided registrar block.
     * Dispatches any dynamic keys associated with the effects to the DataTransformerRegistry.
     *
     * @param registrar A consumer capturing the evaluated resource location and its associated effect map codec.
     */
    public static void registerEffects(BiConsumer<ResourceLocation, MapCodec<? extends SpellFieldEffect>> registrar) {
        registerEffect(registrar, PlaySoundVolumeEffect.KEY, PlaySoundVolumeEffect.CODEC, PlaySoundVolumeEffect.KEY_PROVIDER);
        registerEffect(registrar, PlaySoundEntityEffect.KEY, PlaySoundEntityEffect.CODEC, PlaySoundEntityEffect.KEY_PROVIDER);
        registerEffect(registrar, PlaySoundBlockEffect.KEY, PlaySoundBlockEffect.CODEC, PlaySoundBlockEffect.KEY_PROVIDER);

        registerEffect(registrar, BreakBlockEffect.KEY, BreakBlockEffect.CODEC, BreakBlockEffect.KEY_PROVIDER);
        registerEffect(registrar, IgniteBlockEffect.KEY, IgniteBlockEffect.CODEC, IgniteBlockEffect.KEY_PROVIDER);
        registerEffect(registrar, PlaceBlockEffect.KEY, PlaceBlockEffect.CODEC, PlaceBlockEffect.KEY_PROVIDER);
        registerEffect(registrar, PlaceTemporaryBlockEffect.KEY, PlaceTemporaryBlockEffect.CODEC, PlaceTemporaryBlockEffect.KEY_PROVIDER);
        registerEffect(registrar, TemporaryTransmuteBlockEffect.KEY, TemporaryTransmuteBlockEffect.CODEC, TemporaryTransmuteBlockEffect.KEY_PROVIDER);
        registerEffect(registrar, TransmuteBlockEffect.KEY, TransmuteBlockEffect.CODEC, TransmuteBlockEffect.KEY_PROVIDER);

        registerEffect(registrar, AbsoluteStasisEffect.KEY, AbsoluteStasisEffect.CODEC);
        registerEffect(registrar, AirSupplyEffect.KEY, AirSupplyEffect.CODEC, AirSupplyEffect.KEY_PROVIDER);
        registerEffect(registrar, ApplyPotionEffect.KEY, ApplyPotionEffect.CODEC, ApplyPotionEffect.KEY_PROVIDER);
        registerEffect(registrar, ClearEffectsEffect.KEY, ClearEffectsEffect.CODEC, ClearEffectsEffect.KEY_PROVIDER);
        registerEffect(registrar, DamageEffect.KEY, DamageEffect.CODEC, DamageEffect.KEY_PROVIDER);
        registerEffect(registrar, DisarmEffect.KEY, DisarmEffect.CODEC, DisarmEffect.KEY_PROVIDER);
        registerEffect(registrar, ExtinguishEffect.KEY, ExtinguishEffect.CODEC, ExtinguishEffect.KEY_PROVIDER);
        registerEffect(registrar, HealEffect.KEY, HealEffect.CODEC, HealEffect.KEY_PROVIDER);
        registerEffect(registrar, IgniteEffect.KEY, IgniteEffect.CODEC, IgniteEffect.KEY_PROVIDER);
        registerEffect(registrar, ImpulseEffect.KEY, ImpulseEffect.CODEC, ImpulseEffect.KEY_PROVIDER);
        registerEffect(registrar, OrbitEffect.KEY, OrbitEffect.CODEC, OrbitEffect.KEY_PROVIDER);
        registerEffect(registrar, RandomTeleportEffect.KEY, RandomTeleportEffect.CODEC, RandomTeleportEffect.KEY_PROVIDER);
        registerEffect(registrar, ResetLifetimeEffect.KEY, ResetLifetimeEffect.CODEC);
        registerEffect(registrar, SummonEntityEffect.KEY, SummonEntityEffect.CODEC, SummonEntityEffect.KEY_PROVIDER);
        registerEffect(registrar, SummonLingeringPotionEffect.KEY, SummonLingeringPotionEffect.CODEC, SummonLingeringPotionEffect.KEY_PROVIDER);

        registerEffect(registrar, AmbientFieldVisualEffect.KEY, AmbientFieldVisualEffect.CODEC, AmbientFieldVisualEffect.KEY_PROVIDER);
        registerEffect(registrar, BurstBlockVisualEffect.KEY, BurstBlockVisualEffect.CODEC, BurstBlockVisualEffect.KEY_PROVIDER);
        registerEffect(registrar, BurstEntityVisualEffect.KEY, BurstEntityVisualEffect.CODEC, BurstEntityVisualEffect.KEY_PROVIDER);
        registerEffect(registrar, VectorFieldVisualEffect.KEY, VectorFieldVisualEffect.CODEC, VectorFieldVisualEffect.KEY_PROVIDER);

        registerEffect(registrar, ExplodeEffect.KEY, ExplodeEffect.CODEC, ExplodeEffect.KEY_PROVIDER);
        registerEffect(registrar, LightningStrikeEffect.KEY, LightningStrikeEffect.CODEC, LightningStrikeEffect.KEY_PROVIDER);
        registerEffect(registrar, SurfaceHazardEffect.KEY, SurfaceHazardEffect.CODEC, SurfaceHazardEffect.KEY_PROVIDER);
    }

    private static void registerShape(
            BiConsumer<ResourceLocation, MapCodec<? extends SpellFieldShape>> registrar,
            String path,
            MapCodec<? extends SpellFieldShape> codec,
            LevelBasedKeyProvider keyProvider
    ) {
        if (keyProvider != null) {
            for (String key : keyProvider.getLevelBasedKeys()) {
                DataTransformerRegistry.registerEffectProperty(key);
            }
        }
        registrar.accept(id(path), codec);
    }

    private static void registerEffect(
            BiConsumer<ResourceLocation, MapCodec<? extends SpellFieldEffect>> registrar,
            String path,
            MapCodec<? extends SpellFieldEffect> codec,
            LevelBasedKeyProvider keyProvider
    ) {
        if (keyProvider != null) {
            for (String key : keyProvider.getLevelBasedKeys()) {
                DataTransformerRegistry.registerEffectProperty(key);
            }
        } registerEffect(registrar, path, codec);
    }

    private static void registerEffect(
            BiConsumer<ResourceLocation, MapCodec<? extends SpellFieldEffect>> registrar,
            String path,
            MapCodec<? extends SpellFieldEffect> codec
    ) {
        registrar.accept(id(path), codec);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}