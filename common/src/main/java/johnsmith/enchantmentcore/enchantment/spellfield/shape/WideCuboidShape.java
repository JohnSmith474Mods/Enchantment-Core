package johnsmith.enchantmentcore.enchantment.spellfield.shape;

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

public record WideCuboidShape(
        LevelBasedValue heightDepthRadius,
        LevelBasedValue widthRadius
) implements SpellFieldShape {

    public static final String HEIGHT_DEPTH_RADIUS = "height_depth_radius";
    public static final String WIDTH_RADIUS = "width_radius";

    public static final String KEY = "wide_cuboid";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(HEIGHT_DEPTH_RADIUS, WIDTH_RADIUS);

    public static final MapCodec<WideCuboidShape> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(HEIGHT_DEPTH_RADIUS).forGetter(WideCuboidShape::heightDepthRadius),
            LevelBasedValue.CODEC.fieldOf(WIDTH_RADIUS).forGetter(WideCuboidShape::widthRadius)
    ).apply(instance, WideCuboidShape::new));

    @Override
    public AABB computeBounds(Entity caster, Vec3 epicenter, int enchantmentLevel) {
        float hdR = this.heightDepthRadius.calculate(enchantmentLevel);
        float wR = this.widthRadius.calculate(enchantmentLevel);

        Direction horizontalDir = caster != null ? caster.getDirection() : Direction.NORTH;

        if (horizontalDir.getAxis() == Direction.Axis.X) {
            return new AABB(epicenter.x - hdR, epicenter.y - hdR, epicenter.z - wR,
                    epicenter.x + hdR, epicenter.y + hdR, epicenter.z + wR);
        } else {
            return new AABB(epicenter.x - wR, epicenter.y - hdR, epicenter.z - hdR,
                    epicenter.x + wR, epicenter.y + hdR, epicenter.z + hdR);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldShape> codec() {
        return CODEC;
    }
}