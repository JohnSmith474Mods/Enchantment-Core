package johnsmith.enchantmentcore.enchantment.spellfield.shape;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpellFieldShape;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record CubeShape(
        LevelBasedValue radius
) implements SpellFieldShape {

    public static final String RADIUS = "radius";

    public static final String KEY = "cube";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(RADIUS);

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