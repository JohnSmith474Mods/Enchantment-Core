package johnsmith.enchantmentcore.api.enchantment.spellfield.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldTopology;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpatialVector;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpellFieldShape;
import johnsmith.enchantmentcore.api.registry.EnchantmentCoreRegistries;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Represents a single spatial sub-volume within a {@link johnsmith.enchantmentcore.api.enchantment.spellfield.SpellFieldComponent},
 * pairing an optional outer {@link SpellFieldShape} and {@link SpatialVector} offset with an optional
 * internal {@link FieldTopology}.
 *
 * @param shape    Optional outer bounding box {@link SpellFieldShape}; if absent, bounds are derived from {@link #topology()}.
 * @param offset   Optional {@link SpatialVector} displacement applied to the evaluation epicenter or bounding box.
 * @param topology Optional {@link FieldTopology} governing distance falloff and directional multipliers within the volume.
 */
public record SpellFieldVolume(
        Optional<SpellFieldShape> shape,
        Optional<SpatialVector> offset,
        Optional<FieldTopology> topology
) {
    /** JSON field key for the outer bounding shape. */
    public static final String SHAPE = "shape";

    /** JSON field key for the spatial translation offset. */
    public static final String OFFSET = "offset";

    /** JSON field key for the internal field topology. */
    public static final String TOPOLOGY = "topology";

    /**
     * Supplies the JSON property keys holding {@link net.minecraft.world.item.enchantment.LevelBasedValue}
     * definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> {
        Set<String> keys = new HashSet<>(FieldTopology.KEY_PROVIDER.getLevelBasedKeys());
        keys.addAll(SpatialVector.KEY_PROVIDER.getLevelBasedKeys());
        return keys;
    };

    /**
     * Codec for serializing and deserializing {@link SpellFieldVolume} instances from JSON.
     */
    public static final Codec<SpellFieldVolume> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EnchantmentCoreRegistries.getSpellFieldShapeCodec().optionalFieldOf(SHAPE).forGetter(SpellFieldVolume::shape),
            SpatialVector.CODEC.optionalFieldOf(OFFSET).forGetter(SpellFieldVolume::offset),
            FieldTopology.CODEC.optionalFieldOf(TOPOLOGY).forGetter(SpellFieldVolume::topology)
    ).apply(instance, SpellFieldVolume::new));

    /**
     * Computes the world-space epicenter used for topological evaluation of this volume.
     *
     * @param caster           The reference entity anchoring the volume.
     * @param baseEpicenter    The base world-space epicenter of the parent spell field.
     * @param enchantmentLevel The active level of the enchantment.
     * @return The translated world-space evaluation epicenter.
     */
    public Vec3 computeEvaluationEpicenter(Entity caster, Vec3 baseEpicenter, int enchantmentLevel) {
        if (this.shape.isPresent()) {
            Vec3 translation = this.offset.orElse(SpatialVector.ZERO).resolve(enchantmentLevel, caster);
            return baseEpicenter.add(translation);
        }
        return baseEpicenter;
    }

    /**
     * Computes the world-space axis-aligned bounding box ({@link AABB}) enclosing this volume.
     * <p>
     * Uses {@link #shape()} if present, or falls back to {@link FieldTopology#computeAABB(int, Entity, Vec3)}
     * when only a topology is defined.
     *
     * @param caster           The reference entity anchoring the volume.
     * @param baseEpicenter    The base world-space epicenter of the parent spell field.
     * @param enchantmentLevel The active level of the enchantment.
     * @return The computed world-space {@link AABB}.
     */
    public AABB computeBounds(Entity caster, Vec3 baseEpicenter, int enchantmentLevel) {
        Vec3 translation = this.offset.orElse(SpatialVector.ZERO).resolve(enchantmentLevel, caster);

        if (this.shape.isPresent()) {
            Vec3 volumeCenter = baseEpicenter.add(translation);
            return this.shape.get().computeBounds(caster, volumeCenter, enchantmentLevel);
        } else if (this.topology.isPresent()) {
            AABB baseAABB = this.topology.get().computeAABB(enchantmentLevel, caster, baseEpicenter);
            if (translation.lengthSqr() > 0.0001D) {
                AABB offsetAABB = baseAABB.move(translation);
                if (baseAABB.intersects(offsetAABB)) {
                    return baseAABB.intersect(offsetAABB);
                } else {
                    return new AABB(baseEpicenter, baseEpicenter);
                }
            }
            return baseAABB;
        }

        Vec3 volumeCenter = baseEpicenter.add(translation);
        return new AABB(volumeCenter, volumeCenter);
    }
}