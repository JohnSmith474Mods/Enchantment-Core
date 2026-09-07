package johnsmith.enchantmentcore.enchantment.spellfield.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.SpellFieldComponent;
import johnsmith.enchantmentcore.enchantment.spellfield.entity.AnchorVisualConfig;
import johnsmith.enchantmentcore.enchantment.spellfield.entity.SpellFieldAnchorEntity;
import johnsmith.enchantmentcore.registry.EnchantmentCoreEntities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

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

    public static final String DURATION = "duration";
    public static final String TICK_RATE = "tick_rate";
    public static final String ACTIVATION_DELAY = "activation_delay";
    public static final String ACTIVE_PHASE = "active_phase";
    public static final String REST_PHASE = "rest_phase";
    public static final String SPELL_FIELD = "spell_field";
    public static final String VISUAL_CONFIG = "visual_config";
    public static final String USE_ORIGIN = "use_origin";

    public static final String KEY = "summon_spell_field_anchor";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(
            DURATION, TICK_RATE, ACTIVATION_DELAY, ACTIVE_PHASE, REST_PHASE
    );

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
}