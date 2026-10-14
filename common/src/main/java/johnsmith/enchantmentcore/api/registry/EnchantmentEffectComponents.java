package johnsmith.enchantmentcore.api.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import johnsmith.enchantmentcore.api.enchantment.effect.AreaGravityDamageEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.AutoSmeltEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.BonusLootEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.BowChargeTimeEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.BuoyancyEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.ClimbingEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.DamageHealingEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.DropItemEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.ExperienceYieldEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.ExplosionEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.FluidFogDensityEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.FluidWalkerEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.HealingEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.InvulnerabilityFrameModifierEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.MultiJumpEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.SetFireDurationEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.StreakEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.TransparencyEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.projectile.HomingProjectileEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.projectile.MagneticProjectileEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.projectile.RicochetProjectileEffect;
import johnsmith.enchantmentcore.api.enchantment.effect.projectile.ShrapnelProjectileEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.SpellFieldComponent;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SummonSpellFieldAnchorEffect;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.Unit;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Defines all custom {@link MapCodec}s (for {@link EnchantmentEntityEffect} and {@link EnchantmentLocationBasedEffect})
 * and {@link DataComponentType}s (for enchantment effect components and item stack data components) provided by Enchantment Core.
 * <p>
 * Instantiates components and codecs in memory and registers their {@link LevelBasedKeyProvider}
 * property keys with {@link DataTransformerRegistry} without mutating frozen vanilla {@code BuiltInRegistries}
 * during class initialization. Platform registrars bind these definitions during loader-specific registration events.
 */
public class EnchantmentEffectComponents {
    /** Internal registry map of custom {@link EnchantmentEntityEffect} codecs keyed by path identifier. */
    private static final Map<String, MapCodec<? extends EnchantmentEntityEffect>> ENTITY_EFFECT_TYPES = new LinkedHashMap<>();

    /** Internal registry map of custom {@link EnchantmentLocationBasedEffect} codecs keyed by path identifier. */
    private static final Map<String, MapCodec<? extends EnchantmentLocationBasedEffect>> LOCATION_BASED_EFFECT_TYPES = new LinkedHashMap<>();

    /** Internal registry map of custom enchantment effect {@link DataComponentType}s keyed by path identifier. */
    private static final Map<String, DataComponentType<?>> EFFECT_COMPONENT_TYPES = new LinkedHashMap<>();

    /** Internal registry map of custom item stack {@link DataComponentType}s keyed by path identifier. */
    private static final Map<String, DataComponentType<?>> ITEM_DATA_COMPONENT_TYPES = new LinkedHashMap<>();

    /**
     * Entity effect codec for calculating and assigning a custom fire duration (in seconds) on an entity.
     */
    public static final MapCodec<SetFireDurationEffect> SET_FIRE_DURATION = registerEntityEffect(
            SetFireDurationEffect.KEY,
            SetFireDurationEffect.KEY_PROVIDER,
            SetFireDurationEffect.CODEC
    );

    /**
     * Entity effect codec for triggering configurable explosions on entity interactions.
     */
    public static final MapCodec<ExplosionEffect> EXPLOSION_ENTITY = registerEntityEffect(
            ExplosionEffect.KEY,
            ExplosionEffect.KEY_PROVIDER,
            ExplosionEffect.CODEC
    );

    /**
     * Entity effect codec for restoring health or granting absorption hearts on entity interactions.
     */
    public static final MapCodec<HealingEntityEffect> HEALING_ENTITY_EFFECT = registerEntityEffect(
            HealingEntityEffect.KEY,
            HealingEntityEffect.KEY_PROVIDER,
            HealingEntityEffect.CODEC
    );

    /**
     * Entity effect codec for disarming and dropping equipped items from a target entity.
     */
    public static final MapCodec<DropItemEffect> DROP_ITEM_EFFECT = registerEntityEffect(
            DropItemEffect.KEY,
            DropItemEffect.KEY_PROVIDER,
            DropItemEffect.CODEC
    );

    /**
     * Entity effect codec for evaluating a spatial {@link SpellFieldComponent} and its nested volume, block, entity, and visual effects.
     */
    public static final MapCodec<SpellFieldComponent> SPELL_FIELD = registerEntityEffect(
            SpellFieldComponent.KEY,
            SpellFieldComponent.KEY_PROVIDER,
            SpellFieldComponent.CODEC
    );

    /**
     * Entity effect codec for modifying a target entity's post-hit invulnerability timer ({@code invulnerableTime}).
     */
    public static final MapCodec<InvulnerabilityFrameModifierEffect> INVULNERABILITY_FRAME_MODIFIER_EFFECT = registerEntityEffect(
            InvulnerabilityFrameModifierEffect.KEY,
            InvulnerabilityFrameModifierEffect.KEY_PROVIDER,
            InvulnerabilityFrameModifierEffect.CODEC
    );

    /**
     * Entity effect codec for generating a localized gravitational pull/push field with area damage.
     */
    public static final MapCodec<AreaGravityDamageEffect> AREA_GRAVITY_DAMAGE = registerEntityEffect(
            AreaGravityDamageEffect.KEY,
            AreaGravityDamageEffect.KEY_PROVIDER,
            AreaGravityDamageEffect.CODEC
    );

    /**
     * Entity effect codec for summoning a persistent {@link johnsmith.enchantmentcore.api.enchantment.spellfield.entity.SpellFieldAnchorEntity}.
     */
    public static final MapCodec<SummonSpellFieldAnchorEffect> SUMMON_SPELL_FIELD_ANCHOR = registerEntityEffect(
            SummonSpellFieldAnchorEffect.KEY,
            SummonSpellFieldAnchorEffect.KEY_PROVIDER,
            SummonSpellFieldAnchorEffect.CODEC
    );

    /**
     * Location-based effect codec for triggering configurable explosions at world coordinates.
     */
    public static final MapCodec<ExplosionEffect> EXPLOSION_LOCATION = registerLocationBasedEffect(
            ExplosionEffect.KEY,
            ExplosionEffect.KEY_PROVIDER,
            ExplosionEffect.CODEC
    );

    /**
     * Enchantment effect component for modifying shader fog start and end distances when submerged in matching fluids.
     */
    public static final DataComponentType<FluidFogDensityEffect> FLUID_FOG_DENSITY = registerEffectComponent(
            FluidFogDensityEffect.KEY,
            FluidFogDensityEffect.KEY_PROVIDER,
            builder -> builder.persistent(FluidFogDensityEffect.CODEC)
    );

    /**
     * Enchantment effect component for modifying bow full-charge draw duration in seconds.
     */
    public static final DataComponentType<BowChargeTimeEffect> BOW_CHARGE_TIME = registerEffectComponent(
            BowChargeTimeEffect.KEY,
            BowChargeTimeEffect.KEY_PROVIDER,
            builder -> builder.persistent(BowChargeTimeEffect.CODEC)
    );

    /**
     * Enchantment effect component for modifying initial fired projectile velocity.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_VELOCITY = registerEffectComponent(
            "projectile_velocity",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentValueEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component for modifying aerodynamic and fluid drag on fired projectiles.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_DRAG = registerEffectComponent(
            "projectile_drag",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentValueEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component for modifying movement drag/friction on living entities across air, ground, and fluids.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> ENTITY_DRAG = registerEffectComponent(
            "entity_drag",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentValueEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component for modifying fired projectile inaccuracy/spread.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_ACCURACY = registerEffectComponent(
            "projectile_accuracy",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentValueEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component for scaling entity gravity (primarily used for projectiles).
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> GRAVITY_MODIFIER = registerEffectComponent(
            "gravity_modifier",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentValueEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component enabling frustum-based target acquisition and steering on projectiles.
     */
    public static final DataComponentType<List<ConditionalEffect<HomingProjectileEffect>>> PROJECTILE_HOMING = registerEffectComponent(
            HomingProjectileEffect.KEY,
            HomingProjectileEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            HomingProjectileEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component enabling arrows to bounce off solid block surfaces.
     */
    public static final DataComponentType<List<ConditionalEffect<RicochetProjectileEffect>>> PROJECTILE_RICOCHET = registerEffectComponent(
            RicochetProjectileEffect.KEY,
            RicochetProjectileEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            RicochetProjectileEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component enabling radial magnetic trajectory pull toward nearby entities.
     */
    public static final DataComponentType<List<ConditionalEffect<MagneticProjectileEffect>>> PROJECTILE_MAGNETIC = registerEffectComponent(
            MagneticProjectileEffect.KEY,
            MagneticProjectileEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            MagneticProjectileEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component enabling projectiles to fracture into sub-munition clones upon impact.
     */
    public static final DataComponentType<List<ConditionalEffect<ShrapnelProjectileEffect>>> PROJECTILE_SHRAPNEL = registerEffectComponent(
            ShrapnelProjectileEffect.KEY,
            ShrapnelProjectileEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            ShrapnelProjectileEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component that automatically smelts block drops upon mining.
     */
    public static final DataComponentType<List<ConditionalEffect<AutoSmeltEffect>>> AUTO_SMELT = registerEffectComponent(
            AutoSmeltEffect.KEY,
            AutoSmeltEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            AutoSmeltEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component that multiplies experience dropped from mining blocks and killing mobs.
     */
    public static final DataComponentType<List<ConditionalEffect<ExperienceYieldEffect>>> EXPERIENCE_YIELD_MULTIPLIER = registerEffectComponent(
            ExperienceYieldEffect.KEY,
            ExperienceYieldEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            ExperienceYieldEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component allowing entities to walk across specified fluid surfaces as solid blocks.
     */
    public static final DataComponentType<List<ConditionalEffect<FluidWalkerEffect>>> FLUID_WALKER = registerEffectComponent(
            FluidWalkerEffect.KEY,
            FluidWalkerEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            FluidWalkerEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component that prevents passive sinking in fluids and scales submerged mining speed.
     */
    public static final DataComponentType<List<ConditionalEffect<BuoyancyEffect>>> BUOYANCY = registerEffectComponent(
            BuoyancyEffect.KEY,
            BuoyancyEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            BuoyancyEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component for rolling additional bonus item and XP drops when breaking matching blocks.
     */
    public static final DataComponentType<List<ConditionalEffect<BonusLootEffect>>> BONUS_LOOT = registerEffectComponent(
            BonusLootEffect.KEY,
            BonusLootEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            BonusLootEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment trigger component evaluated on the player immediately after breaking a block.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> POST_MINE = registerEffectComponent(
            "post_mine",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment trigger component evaluated on a living entity immediately before totem death protection checks upon taking fatal damage.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> FATAL_DAMAGE = registerEffectComponent(
            "fatal_damage",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment trigger component evaluated when a living entity jumps from the ground.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> ON_JUMP = registerEffectComponent(
            "on_jump",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment trigger component evaluated when a living entity blocks damage with a shield.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> SHIELD_BLOCK = registerEffectComponent(
            "shield_block",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment trigger component evaluated when a living entity begins using/channeling an item.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> ITEM_USE_START = registerEffectComponent(
            "item_use_start",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment trigger component evaluated each tick while a living entity is actively using/channeling an item.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> ITEM_USE_TICK = registerEffectComponent(
            "item_use_tick",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment trigger component evaluated each server tick for items in active equipment slots within a player's inventory.
     */
    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> INVENTORY_TICK = registerEffectComponent(
            "inventory_tick",
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            EnchantmentEntityEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component granting additional mid-air jumps before touching the ground.
     */
    public static final DataComponentType<List<ConditionalEffect<MultiJumpEffect>>> MULTI_JUMP = registerEffectComponent(
            MultiJumpEffect.KEY,
            MultiJumpEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            MultiJumpEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component restoring health or absorption to the attacker proportional to combat damage dealt.
     */
    public static final DataComponentType<List<ConditionalEffect<DamageHealingEffect>>> ON_DAMAGE_HEALING = registerEffectComponent(
            DamageHealingEffect.KEY,
            DamageHealingEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            DamageHealingEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component enabling entities to climb vertical surfaces.
     */
    public static final DataComponentType<List<ConditionalEffect<ClimbingEffect>>> CLIMBING = registerEffectComponent(
            ClimbingEffect.KEY,
            ClimbingEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            ClimbingEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component controlling visual alpha opacity of rendered equipment and mob armor-detection mitigation.
     */
    public static final DataComponentType<List<ConditionalEffect<TransparencyEffect>>> TRANSPARENCY = registerEffectComponent(
            TransparencyEffect.KEY,
            TransparencyEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            TransparencyEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component granting consecutive mining speed bonuses when breaking identical block types.
     */
    public static final DataComponentType<List<ConditionalEffect<StreakEffect>>> MINING_STREAK = registerEffectComponent(
            StreakEffect.MINING_STREAK_KEY,
            StreakEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            StreakEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Enchantment effect component granting consecutive combat damage bonuses when striking identical entity types.
     */
    public static final DataComponentType<List<ConditionalEffect<StreakEffect>>> DAMAGE_STREAK = registerEffectComponent(
            StreakEffect.DAMAGE_STREAK_KEY,
            StreakEffect.KEY_PROVIDER,
            builder -> builder.persistent(
                    ConditionalEffect.codec(
                            StreakEffect.CODEC
                    ).listOf()
            )
    );

    /**
     * Marker enchantment effect component allowing an entity wearing the enchanted item to walk on powder snow without sinking.
     */
    public static final DataComponentType<Unit> POWDER_SNOW_WALKABLE = registerEffectComponent(
            "powder_snow_walkable",
            builder -> builder.persistent(Unit.CODEC)
    );

    /**
     * Runtime item stack data component used to accumulate fractional smelting experience across auto-smelted block drops.
     */
    public static final DataComponentType<Float> STORED_SMELTING_XP = registerItemComponent(
            "stored_smelting_xp",
            builder -> builder.persistent(Codec.FLOAT)
    );

    /**
     * Enchantment effect component storing a map of extracted {@link LevelBasedValue} curves keyed by pointer ID.
     * <p>
     * Automatically populated at load time by {@code EnchantmentCodecMixin} when scanning for
     * {@link johnsmith.enchantmentcore.api.enchantment.value.PointerValue} nodes.
     */
    public static final DataComponentType<Map<String, LevelBasedValue>> DESCRIPTION_ARGS = registerEffectComponent(
            "description_args",
            builder -> builder.persistent(Codec.unboundedMap(Codec.STRING, LevelBasedValue.CODEC))
    );

    // region Internal Builders
    private static <T extends EnchantmentEntityEffect> MapCodec<T> registerEntityEffect(
            String name,
            LevelBasedKeyProvider keyProvider,
            MapCodec<T> codec
    ) {
        if (keyProvider != null) {
            for (String key : keyProvider.getLevelBasedKeys()) {
                DataTransformerRegistry.registerEffectProperty(key);
            }
        }
        ENTITY_EFFECT_TYPES.put(name, codec);
        return codec;
    }

    private static <T extends EnchantmentLocationBasedEffect> MapCodec<T> registerLocationBasedEffect(
            String name,
            LevelBasedKeyProvider keyProvider,
            MapCodec<T> codec
    ) {
        if (keyProvider != null) {
            for (String key : keyProvider.getLevelBasedKeys()) {
                DataTransformerRegistry.registerEffectProperty(key);
            }
        }
        LOCATION_BASED_EFFECT_TYPES.put(name, codec);
        return codec;
    }

    private static <T> DataComponentType<T> registerEffectComponent(
            String name,
            LevelBasedKeyProvider keyProvider,
            UnaryOperator<DataComponentType.Builder<T>> builderOperator
    ) {
        if (keyProvider != null) {
            for (String key : keyProvider.getLevelBasedKeys()) {
                DataTransformerRegistry.registerEffectProperty(key);
            }
        }
        return registerEffectComponent(name, builderOperator);
    }

    private static <T> DataComponentType<T> registerEffectComponent(
            String name,
            UnaryOperator<DataComponentType.Builder<T>> builderOperator
    ) {
        DataComponentType<T> componentType = builderOperator.apply(DataComponentType.builder()).build();
        EFFECT_COMPONENT_TYPES.put(name, componentType);
        return componentType;
    }

    private static <T> DataComponentType<T> registerItemComponent(
            String name,
            UnaryOperator<DataComponentType.Builder<T>> builderOperator
    ) {
        DataComponentType<T> componentType = builderOperator.apply(DataComponentType.builder()).build();
        ITEM_DATA_COMPONENT_TYPES.put(name, componentType);
        return componentType;
    }
    // endregion

    // region Accessors

    /**
     * Retrieves an unmodifiable view of all defined {@link EnchantmentEntityEffect} codecs keyed by path identifier.
     *
     * @return The map of entity effect codecs.
     */
    public static Map<String, MapCodec<? extends EnchantmentEntityEffect>> getEntityEffectTypes() {
        return Collections.unmodifiableMap(ENTITY_EFFECT_TYPES);
    }

    /**
     * Retrieves an unmodifiable view of all defined {@link EnchantmentLocationBasedEffect} codecs keyed by path identifier.
     *
     * @return The map of location-based effect codecs.
     */
    public static Map<String, MapCodec<? extends EnchantmentLocationBasedEffect>> getLocationBasedEffectTypes() {
        return Collections.unmodifiableMap(LOCATION_BASED_EFFECT_TYPES);
    }

    /**
     * Retrieves an unmodifiable view of all defined enchantment effect {@link DataComponentType}s keyed by path identifier.
     *
     * @return The map of enchantment effect component types.
     */
    public static Map<String, DataComponentType<?>> getEffectComponentTypes() {
        return Collections.unmodifiableMap(EFFECT_COMPONENT_TYPES);
    }

    /**
     * Retrieves an unmodifiable view of all defined item stack {@link DataComponentType}s keyed by path identifier.
     *
     * @return The map of item data component types.
     */
    public static Map<String, DataComponentType<?>> getItemDataComponentTypes() {
        return Collections.unmodifiableMap(ITEM_DATA_COMPONENT_TYPES);
    }
    // endregion

    /**
     * Triggers static initialization of enchantment effect component definitions.
     */
    public static void initialize() {}
}