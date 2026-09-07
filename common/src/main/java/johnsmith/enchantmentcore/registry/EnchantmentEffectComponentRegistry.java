package johnsmith.enchantmentcore.registry;

import java.util.List;
import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.effect.*;
import johnsmith.enchantmentcore.enchantment.effect.projectile.HomingProjectileEffect;
import johnsmith.enchantmentcore.enchantment.effect.projectile.MagneticProjectileEffect;
import johnsmith.enchantmentcore.enchantment.effect.projectile.RicochetProjectileEffect;
import johnsmith.enchantmentcore.enchantment.effect.projectile.ShrapnelProjectileEffect;
import johnsmith.enchantmentcore.enchantment.spellfield.SpellFieldComponent;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.SummonSpellFieldAnchorEffect;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Registry class for all custom {@link MapCodec} (server-side entityEffects) and
 * {@link DataComponentType} (item-based entityEffects/data) used by the mod's enchantment system.
 * <p>
 * This class ensures static initialization and centralizes the registration of all custom
 * enchantment-related components into Minecraft's built-in registries.
 */
public class EnchantmentEffectComponentRegistry {

    /**
     * Registers the effect to calculate and set a custom fire duration on an arrow entity.
     */
    public static final MapCodec<SetFireDurationEffect> SET_FIRE_DURATION = register(
            SetFireDurationEffect.KEY,
            SetFireDurationEffect.KEY_PROVIDER,
            SetFireDurationEffect.CODEC
    );

    /**
     * Registers the component for creating explosions on entity interactions.
     */
    public static final MapCodec<ExplosionEffect> EXPLOSION_ENTITY = register(
            ExplosionEffect.KEY,
            ExplosionEffect.KEY_PROVIDER,
            ExplosionEffect.CODEC
    );

    /**
     * Registers the component for healing on entity interactions.
     */
    public static final MapCodec<HealingEntityEffect> HEALING_ENTITY_EFFECT = register(
            HealingEntityEffect.KEY,
            HealingEntityEffect.KEY_PROVIDER,
            HealingEntityEffect.CODEC
    );

    /**
     * Registers the component for item dropping on entity interactions.
     */
    public static final MapCodec<DropItemEffect> DROP_ITEM_EFFECT = register(
            DropItemEffect.KEY,
            DropItemEffect.KEY_PROVIDER,
            DropItemEffect.CODEC
    );

    /**
     * Registers the general spell field component to evaluate shapes and spatial entityEffects.
     */
    public static final MapCodec<SpellFieldComponent> SPELL_FIELD = register(
            SpellFieldComponent.KEY,
            SpellFieldComponent.KEY_PROVIDER,
            SpellFieldComponent.CODEC
    );

    /**
     * Registers the component for modifying the invulnerability time on entity interactions.
     */
    public static final MapCodec<InvulnerabilityFrameModifierEffect> INVULNERABILITY_FRAME_MODIFIER_EFFECT = register(
            InvulnerabilityFrameModifierEffect.KEY,
            InvulnerabilityFrameModifierEffect.KEY_PROVIDER,
            InvulnerabilityFrameModifierEffect.CODEC
    );

    /**
     * Registers the component for creating gravity spell fields on entity interactions.
     */
    public static final MapCodec<AreaGravityDamageEffect> AREA_GRAVITY_DAMAGE = register(
            AreaGravityDamageEffect.KEY,
            AreaGravityDamageEffect.KEY_PROVIDER,
            AreaGravityDamageEffect.CODEC
    );

    /**
     * Registers the component for summoning a persistent spell field anchor entity.
     */
    public static final MapCodec<SummonSpellFieldAnchorEffect> SUMMON_SPELL_FIELD_ANCHOR = register(
            SummonSpellFieldAnchorEffect.KEY,
            SummonSpellFieldAnchorEffect.KEY_PROVIDER,
            SummonSpellFieldAnchorEffect.CODEC
    );

    /**
     * Helper to register a custom {@link MapCodec} into the {@code ENCHANTMENT_ENTITY_EFFECT_TYPE} registry
     * and push its dynamic LevelBasedValue keys to the DataTransformerRegistry.
     *
     * @param name         The resource path for the effect.
     * @param keyProvider  The key provider which supplies the unique keys relating to {@link LevelBasedValue} objects.
     * @param codec        The codec instance to register.
     * @param <T>          The type of the effect, extending {@link EnchantmentEntityEffect}.
     * @return The registered {@link MapCodec}.
     */
    private static <T extends EnchantmentEntityEffect> MapCodec<T> register(
            String name,
            LevelBasedKeyProvider keyProvider,
            MapCodec<T> codec
    ) {
        for (String key : keyProvider.getLevelBasedKeys()) {
            DataTransformerRegistry.registerEffectProperty(key);
        }
        return register(name, codec);
    }

    /**
     * Helper to register a custom {@link MapCodec} into the {@code ENCHANTMENT_ENTITY_EFFECT_TYPE} registry.
     *
     * @param name   The resource path for the effect.
     * @param codec  The codec instance to register.
     * @param <T>    The type of the effect, extending {@link EnchantmentEntityEffect}.
     * @return The registered {@link MapCodec}.
     */
    private static <T extends EnchantmentEntityEffect> MapCodec<T> register(String name, MapCodec<T> codec) {
        return Registry.register(
                BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name),
                codec
        );
    }

    /**
     * Registers the component for creating explosions on world interactions.
     */
    public static final MapCodec<ExplosionEffect> EXPLOSION_LOCATION = Registry.register(
            BuiltInRegistries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE,
            ResourceLocation.fromNamespaceAndPath(
                    Constants.MOD_ID,
                    ExplosionEffect.KEY
            ),
            ExplosionEffect.CODEC
    );

    /**
     * Registers the component for clearing fluid fog based on enchantment level.
     */
    public static final DataComponentType<FluidFogDensityEffect> FLUID_FOG_DENSITY = register(
            FluidFogDensityEffect.KEY,
            FluidFogDensityEffect.KEY_PROVIDER,
            builder -> builder.persistent(FluidFogDensityEffect.CODEC)
    );

    /**
     * Registers the component for modifying bow charge time, supporting entityEffects like Quick Charge on bows.
     */
    public static final DataComponentType<BowChargeTimeEffect> BOW_CHARGE_TIME = register(
            BowChargeTimeEffect.KEY,
            BowChargeTimeEffect.KEY_PROVIDER,
            builder -> builder.persistent(BowChargeTimeEffect.CODEC)
    );

    /**
     * Registers the component for modifying projectile velocity.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_VELOCITY = register(
            "projectile_velocity",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentValueEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers the component for modifying projectile drag/inertia.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_DRAG = register(
            "projectile_drag",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentValueEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers the component for modifying global entity friction/drag (Fluids, Ground, Air).
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> ENTITY_DRAG = register(
            "entity_drag",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentValueEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers the component for modifying projectile inaccuracy/spread.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_ACCURACY = register(
            "projectile_accuracy",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentValueEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers the component for modifying entity gravity (intended for projectiles).
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> GRAVITY_MODIFIER = register(
            "gravity_modifier",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentValueEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers the component for making projectiles home in on targets.
     */
    public static final DataComponentType<List<ConditionalEffect<HomingProjectileEffect>>> PROJECTILE_HOMING = register(
            HomingProjectileEffect.KEY,
            HomingProjectileEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            HomingProjectileEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers the component for making projectiles ricochet off walls.
     */
    public static final DataComponentType<List<ConditionalEffect<RicochetProjectileEffect>>> PROJECTILE_RICOCHET = register(
            RicochetProjectileEffect.KEY,
            RicochetProjectileEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            RicochetProjectileEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers the component for making projectiles attracted to targets.
     */
    public static final DataComponentType<List<ConditionalEffect<MagneticProjectileEffect>>> PROJECTILE_MAGNETIC = register(
            MagneticProjectileEffect.KEY,
            MagneticProjectileEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            MagneticProjectileEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers the component for making fracture into shrapnel.
     */
    public static final DataComponentType<List<ConditionalEffect<ShrapnelProjectileEffect>>> PROJECTILE_SHRAPNEL = register(
            ShrapnelProjectileEffect.KEY,
            ShrapnelProjectileEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            ShrapnelProjectileEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers the auto-smelt component for use within enchantment JSON definitions.
     */
    public static final DataComponentType<List<ConditionalEffect<AutoSmeltEffect>>> AUTO_SMELT = register(
            AutoSmeltEffect.KEY,
            AutoSmeltEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            AutoSmeltEffect.CODEC,
                            LootContextParamSets.BLOCK
                    ).listOf()
            )
    );

    /**
     * Registers the experience yield multiplier component for use within enchantment JSON definitions.
     */
    public static final DataComponentType<List<ConditionalEffect<ExperienceYieldEffect>>> EXPERIENCE_YIELD_MULTIPLIER = register(
            ExperienceYieldEffect.KEY,
            ExperienceYieldEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            ExperienceYieldEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ITEM
                    ).listOf()
            )
    );

    /**
     * Registers the fluid walker component allowing entities to stand on specified fluids.
     */
    public static final DataComponentType<List<ConditionalEffect<FluidWalkerEffect>>> FLUID_WALKER = register(
            FluidWalkerEffect.KEY,
            FluidWalkerEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            FluidWalkerEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Register the component which removes sinking in fluids.
     */
    public static final DataComponentType<List<ConditionalEffect<BuoyancyEffect>>> BUOYANCY = register(
            BuoyancyEffect.KEY,
            BuoyancyEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            BuoyancyEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers the bonus loot component for evaluating additional item/xp drops upon block breaking.
     */
    public static final DataComponentType<List<ConditionalEffect<BonusLootEffect>>> BONUS_LOOT = register(
            BonusLootEffect.KEY,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            BonusLootEffect.CODEC,
                            LootContextParamSets.BLOCK
                    ).listOf()
            )
    );

    /**
     * Registers a custom trigger for entity entityEffects exclusively evaluated upon breaking a block.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> POST_MINE = register(
            "post_mine",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC,
                            LootContextParamSets.BLOCK
                    ).listOf()
            )
    );

    /**
     * Registers a custom trigger for entity entityEffects exclusively evaluated upon taking fatal damage.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> FATAL_DAMAGE = register(
            "fatal_damage",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers a custom trigger for entity entityEffects exclusively evaluated upon jumping.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> ON_JUMP = register(
            "on_jump",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers a custom trigger for entity entityEffects exclusively evaluated upon blocking with a shield.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> SHIELD_BLOCK = register(
            "shield_block",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers a custom trigger for entity entityEffects exclusively evaluated upon using an item.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> ITEM_USE_START = register(
            "item_use_start",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers a custom trigger for entity entityEffects exclusively evaluated while using an item.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> ITEM_USE_TICK = register(
            "item_use_tick",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers a custom trigger for entity entityEffects exclusively evaluated on inventory tick.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> INVENTORY_TICK = register(
            "inventory_tick",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers the component for allowing midair jumps.
     */
    public static final DataComponentType<List<ConditionalEffect<MultiJumpEffect>>> MULTI_JUMP = register(
            MultiJumpEffect.KEY,
            MultiJumpEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            MultiJumpEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers the component for post-damage proportional healing.
     */
    public static final DataComponentType<List<ConditionalEffect<DamageHealingEffect>>> ON_DAMAGE_HEALING = register(
            DamageHealingEffect.KEY,
            DamageHealingEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            DamageHealingEffect.CODEC,
                            LootContextParamSets.ENCHANTED_DAMAGE
                    ).listOf()
            )
    );

    /**
     * Registers the component for allowing climbing vertical surfaces.
     */
    public static final DataComponentType<List<ConditionalEffect<ClimbingEffect>>> CLIMBING = register(
            ClimbingEffect.KEY,
            ClimbingEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            ClimbingEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ENTITY
                    ).listOf()
            )
    );

    /**
     * Registers the component for rendering model transparency.
     */
    public static final DataComponentType<List<ConditionalEffect<TransparencyEffect>>> TRANSPARENCY = register(
            TransparencyEffect.KEY,
            TransparencyEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            TransparencyEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ITEM
                    ).listOf()
            )
    );

    /**
     * Registers the component for mining streaks.
     */
    public static final DataComponentType<List<ConditionalEffect<StreakEffect>>> MINING_STREAK = register(
            StreakEffect.MINING_STREAK_KEY,
            StreakEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            StreakEffect.CODEC,
                            LootContextParamSets.ENCHANTED_ITEM
                    ).listOf()
            )
    );

    /**
     * Registers the component for damage streaks.
     */
    public static final DataComponentType<List<ConditionalEffect<StreakEffect>>> DAMAGE_STREAK = register(
            StreakEffect.DAMAGE_STREAK_KEY,
            StreakEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            StreakEffect.CODEC,
                            LootContextParamSets.ENCHANTED_DAMAGE
                    ).listOf()
            )
    );

    /**
     * Helper to register a custom {@link DataComponentType} into the {@code ENCHANTMENT_EFFECT_COMPONENT_TYPE} registry
     * while delegating key extraction to the {@link DataTransformerRegistry}.
     *
     * @param name             The resource path for the component.
     * @param keyProvider      The provider supplying keys of {@link LevelBasedValue} configuration targets.
     * @param builderOperator  Operator that applies custom builder rules (e.g., codecs) to the component.
     * @param <T>              The data type of the component.
     * @return The registered {@link DataComponentType}.
     */
    private static <T> DataComponentType<T> register(
            String name,
            LevelBasedKeyProvider keyProvider,
            UnaryOperator<DataComponentType.Builder<T>> builderOperator
    ) {
        for (String key : keyProvider.getLevelBasedKeys()) {
            DataTransformerRegistry.registerEffectProperty(key);
        }
        return register(name, builderOperator);
    }

    /**
     * Helper to register a generic custom {@link DataComponentType} into the {@code ENCHANTMENT_EFFECT_COMPONENT_TYPE} registry.
     *
     * @param name             The resource path for the component.
     * @param builderOperator  Operator that applies custom builder rules to the component.
     * @param <T>              The data type of the component.
     * @return The registered {@link DataComponentType}.
     */
    private static <T> DataComponentType<T> register(
            String name,
            UnaryOperator<DataComponentType.Builder<T>> builderOperator
    ) {
        return Registry.register(
                BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(
                        Constants.MOD_ID,
                        name),
                builderOperator.apply(DataComponentType.builder()).build()
        );
    }

    /**
     * Registers the runtime item data component used to accumulate fractional smelting experience.
     */
    public static final DataComponentType<Float> STORED_SMELTING_XP = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(
                    Constants.MOD_ID,
                    "stored_smelting_xp"
            ), DataComponentType.<Float>builder().persistent(Codec.FLOAT).build()
    );

    /**
     * Initializes the enchantment effect component registry.
     */
    public static void initialize() {}
}