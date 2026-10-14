package johnsmith.enchantmentcore.api.enchantment.spellfield;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.DirectionalSpellFieldEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVisualEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVolumeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.entity.SpellFieldAnchorEntity;
import johnsmith.enchantmentcore.api.enchantment.spellfield.event.SpellFieldEventDispatcher;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldAxis;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldTopology;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.LocalVolume;
import johnsmith.enchantmentcore.api.enchantment.spellfield.shape.SpellFieldVolume;
import johnsmith.enchantmentcore.api.registry.EnchantmentCoreRegistries;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.client.debug.SpellFieldDebugTracker;
import johnsmith.enchantmentcore.config.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Core {@link EnchantmentEntityEffect} component responsible for evaluating spatial volumes,
 * topological distance falloff curves, and dispatching entity, block, volume, and visual spell field effects.
 *
 * @param volumes       The list of geometric volumes and topological falloffs defining the spell field.
 * @param entityEffects Effects applied to individual entities intersecting the evaluated volumes.
 * @param blockEffects  Effects applied to individual block coordinates within the evaluated volumes.
 * @param volumeEffects Effects evaluated globally across the entire composite volume geometry.
 * @param visualEffects Particle and visual effects rendered across the volume geometry.
 */
public record SpellFieldComponent(
        List<SpellFieldVolume> volumes,
        List<SpellFieldEntityEffect> entityEffects,
        List<SpellFieldBlockEffect> blockEffects,
        List<SpellFieldVolumeEffect> volumeEffects,
        List<SpellFieldVisualEffect> visualEffects
) implements EnchantmentEntityEffect {
    /** JSON field key for the list of spatial volumes. */
    public static final String VOLUMES = "volumes";

    /** JSON field key for the list of entity-targeted spell field effects. */
    public static final String ENTITY_EFFECTS = "entity_effects";

    /** JSON field key for the list of block-targeted spell field effects. */
    public static final String BLOCK_EFFECTS = "block_effects";

    /** JSON field key for the list of volume-wide spell field effects. */
    public static final String VOLUME_EFFECTS = "volume_effects";

    /** JSON field key for the list of visual/particle spell field effects. */
    public static final String VISUAL_EFFECTS = "visual_effects";

    /** Registry path identifier for this enchantment entity effect. */
    public static final String KEY = "spell_field";

    /**
     * Supplies the JSON property keys holding {@link net.minecraft.world.item.enchantment.LevelBasedValue}
     * definitions for dynamic data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = SpellFieldVolume.KEY_PROVIDER;

    /**
     * MapCodec for serializing and deserializing {@link SpellFieldComponent} instances from JSON or NBT.
     */
    public static final MapCodec<SpellFieldComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SpellFieldVolume.CODEC.listOf().fieldOf(VOLUMES).forGetter(SpellFieldComponent::volumes),
            EnchantmentCoreRegistries.getSpellFieldEntityEffectCodec().listOf().optionalFieldOf(ENTITY_EFFECTS, List.of()).forGetter(SpellFieldComponent::entityEffects),
            EnchantmentCoreRegistries.getSpellFieldBlockEffectCodec().listOf().optionalFieldOf(BLOCK_EFFECTS, List.of()).forGetter(SpellFieldComponent::blockEffects),
            EnchantmentCoreRegistries.getSpellFieldVolumeEffectCodec().listOf().optionalFieldOf(VOLUME_EFFECTS, List.of()).forGetter(SpellFieldComponent::volumeEffects),
            EnchantmentCoreRegistries.getSpellFieldVisualEffectCodec().listOf().optionalFieldOf(VISUAL_EFFECTS, List.of()).forGetter(SpellFieldComponent::visualEffects)
    ).apply(instance, SpellFieldComponent::new));

    /**
     * Evaluates all configured volumes and dispatches entity, block, volume, and visual effects.
     * <p>
     * When collecting and executing block effects, coordinates residing in chunks that are not actively
     * ticking blocks ({@link ServerLevel#shouldTickBlocksAt(BlockPos)} is {@code false}) are skipped.
     * This prevents spell fields that straddle chunk boundaries from synchronously force-loading unloaded
     * chunks or placing transient blocks into non-ticking border chunks.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param target           The anchor entity or focal point of the spell field.
     * @param origin           The origin position vector passed by the enchantment trigger.
     */
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
                    // Skip positions in unloaded or non-ticking chunks to avoid synchronous chunk loading.
                    if (!level.shouldTickBlocksAt(pos)) return;

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
            // Ensure the chunk is still ticking blocks before querying block states or applying block effects.
            if (!level.shouldTickBlocksAt(pos)) continue;
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
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }

    // region Builder Pattern

    /**
     * Creates a new fluent {@link Builder} for constructing a {@link SpellFieldComponent}.
     *
     * @return A new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for assembling a {@link SpellFieldComponent} programmatically.
     */
    public static final class Builder {
        private final SpellFieldComponent component;

        private Builder() {
            this.component = new SpellFieldComponent(
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        }

        /**
         * Appends a spatial volume definition to the spell field.
         *
         * @param volume The {@link SpellFieldVolume} to add.
         * @return This builder for chaining.
         */
        public Builder addVolume(SpellFieldVolume volume) {
            this.component.volumes.add(volume);
            return this;
        }

        /**
         * Appends an entity-targeted effect to the spell field.
         *
         * @param effect The {@link SpellFieldEntityEffect} to add.
         * @return This builder for chaining.
         */
        public Builder addEntityEffect(SpellFieldEntityEffect effect) {
            this.component.entityEffects.add(effect);
            return this;
        }

        /**
         * Appends a block-targeted effect to the spell field.
         *
         * @param effect The {@link SpellFieldBlockEffect} to add.
         * @return This builder for chaining.
         */
        public Builder addBlockEffect(SpellFieldBlockEffect effect) {
            this.component.blockEffects.add(effect);
            return this;
        }

        /**
         * Appends a volume-wide effect to the spell field.
         *
         * @param effect The {@link SpellFieldVolumeEffect} to add.
         * @return This builder for chaining.
         */
        public Builder addVolumeEffect(SpellFieldVolumeEffect effect) {
            this.component.volumeEffects.add(effect);
            return this;
        }

        /**
         * Appends a visual/particle effect to the spell field.
         *
         * @param effect The {@link SpellFieldVisualEffect} to add.
         * @return This builder for chaining.
         */
        public Builder addVisualEffect(SpellFieldVisualEffect effect) {
            this.component.visualEffects.add(effect);
            return this;
        }

        /**
         * Returns the constructed {@link SpellFieldComponent}.
         *
         * @return The built {@link SpellFieldComponent} instance.
         */
        public SpellFieldComponent build() {
            return this.component;
        }
    }
    // endregion
}