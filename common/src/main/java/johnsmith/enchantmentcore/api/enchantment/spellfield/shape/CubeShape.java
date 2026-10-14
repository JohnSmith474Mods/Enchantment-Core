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
 * A uniform cubic {@link SpellFieldShape} extending equally by {@link #radius()} along all three
 * global axes ($X, Y, Z$) from the evaluation epicenter.
 *
 * @param radius The {@link LevelBasedValue} half-extent (in blocks) of the cube along each axis.
 */
public record CubeShape(
        LevelBasedValue radius
) implements SpellFieldShape {
    /** JSON field key for the cubic half-extent radius. */
    public static final String RADIUS = "radius";

    /** Registry path identifier for this spell field shape. */
    public static final String KEY = "cube";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(RADIUS);

    /**
     * MapCodec for serializing and deserializing {@link CubeShape} instances from JSON.
     */
    public static final MapCodec<CubeShape> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(RADIUS).forGetter(CubeShape::radius)
    ).apply(instance, CubeShape::new));

    @Override
    public AABB computeBounds(Entity owner, Vec3 epicenter, int enchantmentLevel) {
        float r = this.radius.calculate(enchantmentLevel);
        return new AABB(
                epicenter.x - r, epicenter.y - r, epicenter.z - r,
                epicenter.x + r, epicenter.y + r, epicenter.z + r
        );
    }

    @Override
    public MapCodec<? extends SpellFieldShape> codec() {
        return CODEC;
    }
}