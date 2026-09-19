package johnsmith.enchantmentcore.enchantment.spellfield;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVisualEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVolumeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.DirectionalSpellFieldEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.event.SpellFieldEventDispatcher;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldAxis;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.LocalVolume;
import johnsmith.enchantmentcore.client.debug.SpellFieldDebugTracker;
import johnsmith.enchantmentcore.enchantment.spellfield.entity.SpellFieldAnchorEntity;
import johnsmith.enchantmentcore.enchantment.spellfield.math.FieldTopology;
import johnsmith.enchantmentcore.enchantment.spellfield.shape.SpellFieldVolume;
import johnsmith.enchantmentcore.registry.EnchantmentCoreRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record SpellFieldComponent(
        List<SpellFieldVolume> volumes,
        List<SpellFieldEntityEffect> entityEffects,
        List<SpellFieldBlockEffect> blockEffects,
        List<SpellFieldVolumeEffect> volumeEffects,
        List<SpellFieldVisualEffect> visualEffects
) implements EnchantmentEntityEffect {
    public static final String VOLUMES = "volumes";
    public static final String ENTITY_EFFECTS = "entity_effects";
    public static final String BLOCK_EFFECTS = "block_effects";
    public static final String VOLUME_EFFECTS = "volume_effects";
    public static final String VISUAL_EFFECTS = "visual_effects";

    public static final String KEY = "spell_field";

    public static final LevelBasedKeyProvider KEY_PROVIDER = SpellFieldVolume.KEY_PROVIDER;

    public static final MapCodec<SpellFieldComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SpellFieldVolume.CODEC.listOf().fieldOf(VOLUMES).forGetter(SpellFieldComponent::volumes),
            EnchantmentCoreRegistries.getSpellFieldEntityEffectCodec().listOf().optionalFieldOf(ENTITY_EFFECTS, List.of()).forGetter(SpellFieldComponent::entityEffects),
            EnchantmentCoreRegistries.getSpellFieldBlockEffectCodec().listOf().optionalFieldOf(BLOCK_EFFECTS, List.of()).forGetter(SpellFieldComponent::blockEffects),
            EnchantmentCoreRegistries.getSpellFieldVolumeEffectCodec().listOf().optionalFieldOf(VOLUME_EFFECTS, List.of()).forGetter(SpellFieldComponent::volumeEffects),
            EnchantmentCoreRegistries.getSpellFieldVisualEffectCodec().listOf().optionalFieldOf(VISUAL_EFFECTS, List.of()).forGetter(SpellFieldComponent::visualEffects)
    ).apply(instance, SpellFieldComponent::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Vec3 origin) {
        Vec3 epicenter = target.position().add(0, target.getBbHeight() / 2.0, 0);

        if (!SpellFieldEventDispatcher.dispatchApply(level, enchantmentLevel, context, target, epicenter)) {
            return;
        }

        Vec3 anchorPos = target.position();
        Map<Entity, Float> victimScalars = new HashMap<>();
        Map<BlockPos, Float> blockScalars = new HashMap<>();
        List<GlobalVolume> worldVolumes = new ArrayList<>();
        List<LocalVolume> debugVolumes = new ArrayList<>();

        for (SpellFieldVolume volume : this.volumes) {
            Vec3 evalCenter = volume.computeEvaluationEpicenter(target, epicenter, enchantmentLevel);
            AABB worldBox = volume.computeBounds(target, epicenter, enchantmentLevel);
            AABB localBox = worldBox.move(-anchorPos.x, -anchorPos.y, -anchorPos.z);
            Vec3 localEvalCenter = evalCenter.subtract(anchorPos);
            FieldTopology activeTopology = volume.topology().orElse(null);

            worldVolumes.add(new GlobalVolume(worldBox, activeTopology, evalCenter));
            debugVolumes.add(new LocalVolume(localBox, activeTopology, localEvalCenter));

            if (!this.entityEffects.isEmpty()) {
                for (Entity victim : level.getEntities(context.owner(), worldBox, e -> e.isAlive() && !(e instanceof SpellFieldAnchorEntity))) {
                    float s = activeTopology != null
                            ? activeTopology.evaluateMultiplier(enchantmentLevel, target, evalCenter, victim.position())
                            : 1.0F;

                    s = SpellFieldEventDispatcher.dispatchEntityTarget(level, enchantmentLevel, context, target, victim, s);
                    if (s > 0.0001F) {
                        victimScalars.merge(victim, s, Math::max);
                    }
                }
            }

            if (!this.blockEffects.isEmpty()) {
                BlockPos.betweenClosedStream(
                        BlockPos.containing(worldBox.minX, worldBox.minY, worldBox.minZ),
                        BlockPos.containing(worldBox.maxX, worldBox.maxY, worldBox.maxZ)
                ).forEach(pos -> {
                    float s = activeTopology != null
                            ? activeTopology.evaluateMultiplier(enchantmentLevel, target, evalCenter, Vec3.atCenterOf(pos))
                            : 1.0F;

                    s = SpellFieldEventDispatcher.dispatchBlockTarget(level, enchantmentLevel, context, target, pos.immutable(), s);
                    if (s > 0.0001F) {
                        blockScalars.merge(pos.immutable(), s, Math::max);
                    }
                });
            }
        }

        List<FieldAxis> vectorFields = new ArrayList<>();
        for (SpellFieldEntityEffect effect : this.entityEffects) {
            if (effect instanceof DirectionalSpellFieldEffect directional) {
                directional.getActiveAxis().ifPresent(vectorFields::add);
            }
        }
        for (SpellFieldBlockEffect effect : this.blockEffects) {
            if (effect instanceof DirectionalSpellFieldEffect directional) {
                directional.getActiveAxis().ifPresent(vectorFields::add);
            }
        }
        for (SpellFieldVolumeEffect effect : this.volumeEffects) {
            if (effect instanceof DirectionalSpellFieldEffect directional) {
                directional.getActiveAxis().ifPresent(vectorFields::add);
            }
        }

        SpellFieldDebugTracker.addShapes(target, debugVolumes, vectorFields, enchantmentLevel, Config.RENDER_RETENTION_TICKS.get());

        for (Map.Entry<Entity, Float> entry : victimScalars.entrySet()) {
            float scalar = entry.getValue();
            if (scalar <= 0.0001F) continue;

            for (SpellFieldEntityEffect effect : this.entityEffects) {
                effect.apply(level, enchantmentLevel, context, target, epicenter, entry.getKey(), scalar);
            }
        }

        List<SpellFieldBlockEffect> sortedBlockEffects = new ArrayList<>(this.blockEffects);
        sortedBlockEffects.sort((a, b) -> Float.compare(b.getPriority(enchantmentLevel), a.getPriority(enchantmentLevel)));

        Set<BlockPos> modifiedBlocks = new HashSet<>();
        List<Block> blockBlacklist = Config.SPELL_FIELD_BLOCK_BLACKLIST.get();

        for (Map.Entry<BlockPos, Float> entry : blockScalars.entrySet()) {
            float scalar = entry.getValue();
            if (scalar <= 0.0001F) continue;

            BlockPos pos = entry.getKey();
            if (blockBlacklist.contains(level.getBlockState(pos).getBlock())) continue;

            for (SpellFieldBlockEffect blockEffect : sortedBlockEffects) {
                blockEffect.apply(level, enchantmentLevel, context, target, epicenter, pos, scalar, modifiedBlocks);
            }
        }

        if (!this.volumeEffects.isEmpty()) {
            for (SpellFieldVolumeEffect volumeEffect : this.volumeEffects) {
                volumeEffect.apply(level, enchantmentLevel, context, target, epicenter, worldVolumes);
            }
        }

        if (!this.visualEffects.isEmpty()) {
            for (SpellFieldVisualEffect visualEffect : this.visualEffects) {
                visualEffect.apply(level, enchantmentLevel, context, target, epicenter, worldVolumes, vectorFields);
            }
        }
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() { return CODEC; }
}