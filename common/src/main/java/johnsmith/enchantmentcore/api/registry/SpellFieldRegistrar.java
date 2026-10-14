package johnsmith.enchantmentcore.api.registry;

import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.audio.PlaySoundBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.audio.PlaySoundEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.audio.PlaySoundVolumeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.block.BreakBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.block.IgniteBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.block.PlaceBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.block.PlaceTemporaryBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.block.TemporaryTransmuteBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.block.TransmuteBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.AbsoluteStasisEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.AirSupplyEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.ApplyPotionEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.ClearEffectsEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.DamageEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.DisarmEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.ExtinguishEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.HealEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.IgniteEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.ImpulseEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.OrbitEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.RandomTeleportEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.ResetLifetimeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.SummonEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.SummonLingeringPotionEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.visual.AmbientFieldVisualEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.visual.BurstBlockVisualEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.visual.BurstEntityVisualEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.visual.VectorFieldVisualEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.volume.ExplodeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.volume.LightningStrikeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.volume.SurfaceHazardEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpellFieldShape;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.function.ConstantScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.function.ExponentialScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.function.LinearScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.function.LogarithmicScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.function.PolynomialScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.function.SigmoidScalingFunction;
import johnsmith.enchantmentcore.api.enchantment.spellfield.shape.CubeShape;
import johnsmith.enchantmentcore.api.enchantment.spellfield.shape.CuboidShape;
import johnsmith.enchantmentcore.api.enchantment.spellfield.shape.DeepCuboidShape;
import johnsmith.enchantmentcore.api.enchantment.spellfield.shape.WideCuboidShape;
import net.minecraft.resources.ResourceLocation;

/**
 * Centralized registration helper for binding built-in spell field {@link DistanceScalingFunction}s,
 * {@link SpellFieldShape}s, and {@link SpellFieldEffect}s to their custom registries across all mod loaders.
 * <p>
 * Also dispatches each implementation's {@link LevelBasedKeyProvider} keys to {@link DataTransformerRegistry}
 * so that nested level-based values inside spell fields are automatically recognized during import and export.
 */
public class SpellFieldRegistrar {

    /**
     * Feeds all built-in {@link DistanceScalingFunction} codecs into the provided platform registrar callback.
     *
     * @param registrar A consumer accepting each function's {@link ResourceLocation} and {@link MapCodec}.
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
     * Feeds all built-in {@link SpellFieldShape} codecs into the provided platform registrar callback
     * and registers their level-based JSON keys with {@link DataTransformerRegistry}.
     *
     * @param registrar A consumer accepting each shape's {@link ResourceLocation} and {@link MapCodec}.
     */
    public static void registerShapes(BiConsumer<ResourceLocation, MapCodec<? extends SpellFieldShape>> registrar) {
        registerShape(registrar, CubeShape.KEY, CubeShape.CODEC, CubeShape.KEY_PROVIDER);
        registerShape(registrar, CuboidShape.KEY, CuboidShape.CODEC, CuboidShape.KEY_PROVIDER);
        registerShape(registrar, DeepCuboidShape.KEY, DeepCuboidShape.CODEC, DeepCuboidShape.KEY_PROVIDER);
        registerShape(registrar, WideCuboidShape.KEY, WideCuboidShape.CODEC, WideCuboidShape.KEY_PROVIDER);
    }

    /**
     * Feeds all built-in {@link SpellFieldEffect} codecs into the provided platform registrar callback
     * and registers their level-based JSON keys with {@link DataTransformerRegistry}.
     *
     * @param registrar A consumer accepting each effect's {@link ResourceLocation} and {@link MapCodec}.
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
        }
        registerEffect(registrar, path, codec);
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