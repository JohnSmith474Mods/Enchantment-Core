package johnsmith.enchantmentcore.enchantment.spellfield.shape;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpellFieldShape;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpatialVector;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.math.FieldTopology;
import johnsmith.enchantmentcore.registry.EnchantmentCoreRegistries;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record SpellFieldVolume(
        Optional<SpellFieldShape> shape,
        Optional<SpatialVector> offset,
        Optional<FieldTopology> topology
) {
    public static final String SHAPE = "shape";
    public static final String OFFSET = "offset";
    public static final String TOPOLOGY = "topology";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> {
        Set<String> keys = new HashSet<>(FieldTopology.KEY_PROVIDER.getLevelBasedKeys());
        keys.addAll(SpatialVector.KEY_PROVIDER.getLevelBasedKeys());
        return keys;
    };

    public static final Codec<SpellFieldVolume> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EnchantmentCoreRegistries.getSpellFieldShapeCodec().optionalFieldOf(SHAPE).forGetter(SpellFieldVolume::shape),
            SpatialVector.CODEC.optionalFieldOf(OFFSET).forGetter(SpellFieldVolume::offset),
            FieldTopology.CODEC.optionalFieldOf(TOPOLOGY).forGetter(SpellFieldVolume::topology)
    ).apply(instance, SpellFieldVolume::new));

    public Vec3 computeEvaluationEpicenter(Entity caster, Vec3 baseEpicenter, int enchantmentLevel) {
        if (this.shape.isPresent()) {
            Vec3 translation = this.offset.orElse(SpatialVector.ZERO).resolve(enchantmentLevel, caster);
            return baseEpicenter.add(translation);
        }
        return baseEpicenter;
    }

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