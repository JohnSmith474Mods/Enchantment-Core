package johnsmith.enchantmentcore.api.enchantment.spellfield.shape;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpellFieldShape;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * An orientation-aware cuboid {@link SpellFieldShape} that aligns its {@link #depthRadius()} with the
 * nearest cardinal axis (including vertical up/down) of the caster's 3D look vector, while applying
 * {@link #heightWidthRadius()} to the two orthogonal cross-sectional axes (ideal for 3D tunnel mining).
 *
 * @param heightWidthRadius The {@link LevelBasedValue} half-extent applied to the two axes perpendicular to the look axis.
 * @param depthRadius       The {@link LevelBasedValue} half-extent applied along the primary cardinal look axis.
 */
public record DeepCuboidShape(
        LevelBasedValue heightWidthRadius,
        LevelBasedValue depthRadius
) implements SpellFieldShape {
    /** JSON field key for the cross-sectional height/width half-extent. */
    public static final String HEIGHT_WIDTH_RADIUS = "height_width_radius";

    /** JSON field key for the longitudinal depth half-extent. */
    public static final String DEPTH_RADIUS = "depth_radius";

    /** Registry path identifier for this spell field shape. */
    public static final String KEY = "deep_cuboid";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(HEIGHT_WIDTH_RADIUS, DEPTH_RADIUS);

    /**
     * MapCodec for serializing and deserializing {@link DeepCuboidShape} instances from JSON.
     */
    public static final MapCodec<DeepCuboidShape> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(HEIGHT_WIDTH_RADIUS).forGetter(DeepCuboidShape::heightWidthRadius),
            LevelBasedValue.CODEC.fieldOf(DEPTH_RADIUS).forGetter(DeepCuboidShape::depthRadius)
    ).apply(instance, DeepCuboidShape::new));

    @Override
    public AABB computeBounds(Entity caster, Vec3 epicenter, int enchantmentLevel) {
        float hwR = this.heightWidthRadius.calculate(enchantmentLevel);
        float dR = this.depthRadius.calculate(enchantmentLevel);

        Direction lookDir = caster != null ? Direction.getNearest(
                caster.getViewVector(1.0F).x,
                caster.getViewVector(1.0F).y,
                caster.getViewVector(1.0F).z
        ) : Direction.DOWN;

        if (lookDir.getAxis() == Direction.Axis.X) {
            return new AABB(epicenter.x - dR, epicenter.y - hwR, epicenter.z - hwR,
                    epicenter.x + dR, epicenter.y + hwR, epicenter.z + hwR);
        } else if (lookDir.getAxis() == Direction.Axis.Y) {
            return new AABB(epicenter.x - hwR, epicenter.y - dR, epicenter.z - hwR,
                    epicenter.x + hwR, epicenter.y + dR, epicenter.z + hwR);
        } else {
            return new AABB(epicenter.x - hwR, epicenter.y - hwR, epicenter.z - dR,
                    epicenter.x + hwR, epicenter.y + hwR, epicenter.z + dR);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldShape> codec() {
        return CODEC;
    }
}