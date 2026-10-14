package johnsmith.enchantmentcore.api.enchantment.spellfield.shape;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpellFieldShape;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * An axis-aligned rectangular prism {@link SpellFieldShape} with independent half-extents along the
 * global $X$, $Y$, and $Z$ axes.
 *
 * @param xRadius The {@link LevelBasedValue} half-extent along the global $X$ axis.
 * @param yRadius The {@link LevelBasedValue} half-extent along the global $Y$ axis.
 * @param zRadius The {@link LevelBasedValue} half-extent along the global $Z$ axis.
 */
public record CuboidShape(
        LevelBasedValue xRadius,
        LevelBasedValue yRadius,
        LevelBasedValue zRadius
) implements SpellFieldShape {
    /** JSON field key for the X-axis half-extent. */
    public static final String X_RADIUS = "x_radius";

    /** JSON field key for the Y-axis half-extent. */
    public static final String Y_RADIUS = "y_radius";

    /** JSON field key for the Z-axis half-extent. */
    public static final String Z_RADIUS = "z_radius";

    /** Registry path identifier for this spell field shape. */
    public static final String KEY = "cuboid";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(X_RADIUS, Y_RADIUS, Z_RADIUS);

    /**
     * MapCodec for serializing and deserializing {@link CuboidShape} instances from JSON.
     */
    public static final MapCodec<CuboidShape> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(X_RADIUS).forGetter(CuboidShape::xRadius),
            LevelBasedValue.CODEC.fieldOf(Y_RADIUS).forGetter(CuboidShape::yRadius),
            LevelBasedValue.CODEC.fieldOf(Z_RADIUS).forGetter(CuboidShape::zRadius)
    ).apply(instance, CuboidShape::new));

    @Override
    public AABB computeBounds(Entity owner, Vec3 epicenter, int enchantmentLevel) {
        float xR = this.xRadius.calculate(enchantmentLevel);
        float yR = this.yRadius.calculate(enchantmentLevel);
        float zR = this.zRadius.calculate(enchantmentLevel);

        return new AABB(
                epicenter.x - xR, epicenter.y - yR, epicenter.z - zR,
                epicenter.x + xR, epicenter.y + yR, epicenter.z + zR
        );
    }

    @Override
    public MapCodec<? extends SpellFieldShape> codec() {
        return CODEC;
    }
}