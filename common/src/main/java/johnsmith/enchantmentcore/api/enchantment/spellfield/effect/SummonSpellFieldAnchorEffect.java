package johnsmith.enchantmentcore.api.enchantment.spellfield.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.SpellFieldComponent;
import johnsmith.enchantmentcore.api.enchantment.spellfield.entity.AnchorVisualConfig;
import johnsmith.enchantmentcore.api.enchantment.spellfield.entity.SpellFieldAnchorEntity;
import johnsmith.enchantmentcore.api.registry.EnchantmentCoreEntities;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

/**
 * An {@link EnchantmentEntityEffect} that spawns a persistent {@link SpellFieldAnchorEntity} in the
 * world to repeatedly or delayedly evaluate an embedded {@link SpellFieldComponent} over time.
 *
 * @param duration        The {@link LevelBasedValue} total lifespan of the anchor entity in ticks.
 * @param tickRate        The {@link LevelBasedValue} interval in ticks between successive spell field evaluations.
 * @param activationDelay The {@link LevelBasedValue} initial delay in ticks before the anchor begins evaluating its spell field.
 * @param activePhase     The {@link LevelBasedValue} duration in ticks of each active pulsing window (or {@code <= 0} for continuous).
 * @param restPhase       The {@link LevelBasedValue} duration in ticks of each dormant rest window between active phases.
 * @param spellField      The {@link SpellFieldComponent} executed by the anchor on each active tick.
 * @param visualConfig    Optional {@link AnchorVisualConfig} controlling billboard sprite or custom 3D model rendering for the anchor.
 * @param useOrigin       If {@code true}, spawns the anchor at the trigger's {@code origin} vector; if {@code false}, spawns at {@code target.position()}.
 */
public record SummonSpellFieldAnchorEffect(
        LevelBasedValue duration,
        LevelBasedValue tickRate,
        LevelBasedValue activationDelay,
        LevelBasedValue activePhase,
        LevelBasedValue restPhase,
        SpellFieldComponent spellField,
        Optional<AnchorVisualConfig> visualConfig,
        boolean useOrigin
) implements EnchantmentEntityEffect {
    /** JSON field key for the anchor lifespan in ticks. */
    public static final String DURATION = "duration";

    /** JSON field key for the evaluation tick interval. */
    public static final String TICK_RATE = "tick_rate";

    /** JSON field key for the initial activation delay in ticks. */
    public static final String ACTIVATION_DELAY = "activation_delay";

    /** JSON field key for the active duty-cycle window in ticks. */
    public static final String ACTIVE_PHASE = "active_phase";

    /** JSON field key for the dormant rest window in ticks. */
    public static final String REST_PHASE = "rest_phase";

    /** JSON field key for the nested spell field component definition. */
    public static final String SPELL_FIELD = "spell_field";

    /** JSON field key for the optional client visual configuration. */
    public static final String VISUAL_CONFIG = "visual_config";

    /** JSON field key for spawning at the trigger origin instead of the target entity position. */
    public static final String USE_ORIGIN = "use_origin";

    /** Registry path identifier for this enchantment entity effect. */
    public static final String KEY = "summon_spell_field_anchor";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            DURATION, TICK_RATE, ACTIVATION_DELAY, ACTIVE_PHASE, REST_PHASE
    );

    /**
     * MapCodec for serializing and deserializing {@link SummonSpellFieldAnchorEffect} instances from JSON.
     */
    public static final MapCodec<SummonSpellFieldAnchorEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(DURATION).forGetter(SummonSpellFieldAnchorEffect::duration),
            LevelBasedValue.CODEC.optionalFieldOf(TICK_RATE, LevelBasedValue.constant(1.0F)).forGetter(SummonSpellFieldAnchorEffect::tickRate),
            LevelBasedValue.CODEC.optionalFieldOf(ACTIVATION_DELAY, LevelBasedValue.constant(0.0F)).forGetter(SummonSpellFieldAnchorEffect::activationDelay),
            LevelBasedValue.CODEC.optionalFieldOf(ACTIVE_PHASE, LevelBasedValue.constant(0.0F)).forGetter(SummonSpellFieldAnchorEffect::activePhase),
            LevelBasedValue.CODEC.optionalFieldOf(REST_PHASE, LevelBasedValue.constant(0.0F)).forGetter(SummonSpellFieldAnchorEffect::restPhase),
            SpellFieldComponent.CODEC.fieldOf(SPELL_FIELD).forGetter(SummonSpellFieldAnchorEffect::spellField),
            AnchorVisualConfig.CODEC.optionalFieldOf(VISUAL_CONFIG).forGetter(SummonSpellFieldAnchorEffect::visualConfig),
            Codec.BOOL.optionalFieldOf(USE_ORIGIN, false).forGetter(SummonSpellFieldAnchorEffect::useOrigin)
    ).apply(instance, SummonSpellFieldAnchorEffect::new));

    /**
     * Instantiates, configures, and spawns a {@link SpellFieldAnchorEntity} in the server level,
     * inheriting the yaw and pitch orientation of {@code context.owner()} (or {@code target} if no owner is present).
     *
     * @param level            The server level in which the anchor is spawned.
     * @param enchantmentLevel The active level of the source enchantment.
     * @param context          The context of the enchanted item in use.
     * @param target           The target entity at which the effect was triggered.
     * @param origin           The origin coordinate vector of the trigger.
     */
    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Vec3 origin) {
        int time = (int) this.duration.calculate(enchantmentLevel);
        int rate = Math.max(1, (int) this.tickRate.calculate(enchantmentLevel));
        int delay = Math.max(0, (int) this.activationDelay.calculate(enchantmentLevel));
        int active = Math.max(0, (int) this.activePhase.calculate(enchantmentLevel));
        int rest = Math.max(0, (int) this.restPhase.calculate(enchantmentLevel));

        Vec3 spawnPos;
        if (this.useOrigin && origin != null) {
            spawnPos = origin;
        } else {
            spawnPos = target != null ? target.position() : origin;
        }

        if (spawnPos == null) {
            spawnPos = Vec3.ZERO;
        }

        SpellFieldAnchorEntity anchor = new SpellFieldAnchorEntity(EnchantmentCoreEntities.SPELL_FIELD_ANCHOR, level);
        anchor.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        Entity referenceEntity = context.owner() != null ? context.owner() : target;
        if (referenceEntity != null) {
            anchor.setYRot(referenceEntity.getYRot());
            anchor.setXRot(referenceEntity.getXRot());
            anchor.setYHeadRot(referenceEntity.getYHeadRot());
        }

        anchor.setAnchorData(time, rate, delay, active, rest, this.spellField, enchantmentLevel, context.itemStack(), context.inSlot(), context.owner());
        this.visualConfig.ifPresent(anchor::setVisualConfig);
        level.addFreshEntity(anchor);
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }

    // region Wither Pattern

    /**
     * Constructs a default {@link SummonSpellFieldAnchorEffect} with a 1-tick duration, 1-tick rate,
     * no delays, and an empty {@link SpellFieldComponent}.
     */
    public SummonSpellFieldAnchorEffect() {
        this(LevelBasedValue.constant(1), LevelBasedValue.constant(1), LevelBasedValue.constant(0), LevelBasedValue.constant(0), LevelBasedValue.constant(0), SpellFieldComponent.builder().build(), Optional.empty(), false);
    }

    /**
     * Returns a copy of this effect with the specified anchor lifespan duration.
     *
     * @param duration The new {@link LevelBasedValue} duration in ticks.
     * @return A new {@link SummonSpellFieldAnchorEffect} instance.
     */
    public SummonSpellFieldAnchorEffect withDuration(LevelBasedValue duration) {
        return new SummonSpellFieldAnchorEffect(duration, tickRate, activationDelay, activePhase, restPhase, spellField, visualConfig, useOrigin);
    }

    /**
     * Returns a copy of this effect with the specified evaluation tick interval.
     *
     * @param tickRate The new {@link LevelBasedValue} tick rate.
     * @return A new {@link SummonSpellFieldAnchorEffect} instance.
     */
    public SummonSpellFieldAnchorEffect withTickRate(LevelBasedValue tickRate) {
        return new SummonSpellFieldAnchorEffect(duration, tickRate, activationDelay, activePhase, restPhase, spellField, visualConfig, useOrigin);
    }

    /**
     * Returns a copy of this effect with the specified initial activation delay.
     *
     * @param activationDelay The new {@link LevelBasedValue} activation delay in ticks.
     * @return A new {@link SummonSpellFieldAnchorEffect} instance.
     */
    public SummonSpellFieldAnchorEffect withActivationDelay(LevelBasedValue activationDelay) {
        return new SummonSpellFieldAnchorEffect(duration, tickRate, activationDelay, activePhase, restPhase, spellField, visualConfig, useOrigin);
    }

    /**
     * Returns a copy of this effect with the specified active phase duration.
     *
     * @param activePhase The new {@link LevelBasedValue} active phase window in ticks.
     * @return A new {@link SummonSpellFieldAnchorEffect} instance.
     */
    public SummonSpellFieldAnchorEffect withActivePhase(LevelBasedValue activePhase) {
        return new SummonSpellFieldAnchorEffect(duration, tickRate, activationDelay, activePhase, restPhase, spellField, visualConfig, useOrigin);
    }

    /**
     * Returns a copy of this effect with the specified dormant rest phase duration.
     *
     * @param restPhase The new {@link LevelBasedValue} rest phase window in ticks.
     * @return A new {@link SummonSpellFieldAnchorEffect} instance.
     */
    public SummonSpellFieldAnchorEffect withRestPhase(LevelBasedValue restPhase) {
        return new SummonSpellFieldAnchorEffect(duration, tickRate, activationDelay, activePhase, restPhase, spellField, visualConfig, useOrigin);
    }

    /**
     * Returns a copy of this effect with the specified {@link SpellFieldComponent} payload.
     *
     * @param spellField The {@link SpellFieldComponent} to evaluate.
     * @return A new {@link SummonSpellFieldAnchorEffect} instance.
     */
    public SummonSpellFieldAnchorEffect withSpellFieldComponent(SpellFieldComponent spellField) {
        return new SummonSpellFieldAnchorEffect(duration, tickRate, activationDelay, activePhase, restPhase, spellField, visualConfig, useOrigin);
    }

    /**
     * Returns a copy of this effect with the specified client visual configuration.
     *
     * @param visualConfig The {@link AnchorVisualConfig} to attach.
     * @return A new {@link SummonSpellFieldAnchorEffect} instance.
     */
    public SummonSpellFieldAnchorEffect withVisualConfig(AnchorVisualConfig visualConfig) {
        return new SummonSpellFieldAnchorEffect(duration, tickRate, activationDelay, activePhase, restPhase, spellField, Optional.of(visualConfig), useOrigin);
    }

    /**
     * Returns a copy of this effect configured to spawn at either the trigger origin or the target entity's position.
     *
     * @param useOrigin {@code true} to use the trigger origin coordinate, {@code false} to use the target's position.
     * @return A new {@link SummonSpellFieldAnchorEffect} instance.
     */
    public SummonSpellFieldAnchorEffect withUseOrigin(boolean useOrigin) {
        return new SummonSpellFieldAnchorEffect(duration, tickRate, activationDelay, activePhase, restPhase, spellField, visualConfig, useOrigin);
    }
    // endregion
}