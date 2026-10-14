package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.visual;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A visual {@link SpellFieldEntityEffect} that emits a burst of particles centered at the vertical
 * midpoint of each targeted entity within a spell field volume, scaling both particle count and
 * speed by the entity's local topological multiplier.
 *
 * @param particle The {@link ParticleOptions} defining the particle type to emit.
 * @param count    The {@link LevelBasedValue} base number of particles emitted per entity before topological scaling.
 * @param speed    The {@link LevelBasedValue} base particle speed before topological scaling.
 */
public record BurstEntityVisualEffect(
        ParticleOptions particle,
        LevelBasedValue count,
        LevelBasedValue speed
) implements SpellFieldEntityEffect {
    /** JSON field key for the particle options definition. */
    public static final String PARTICLE = "particle";

    /** JSON field key for the base particle count. */
    public static final String COUNT = "count";

    /** JSON field key for the base particle speed. */
    public static final String SPEED = "speed";

    /** Registry path identifier for this spell field entity effect. */
    public static final String KEY = "burst_entity";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(COUNT, SPEED);

    /**
     * MapCodec for serializing and deserializing {@link BurstEntityVisualEffect} instances from JSON.
     */
    public static final MapCodec<BurstEntityVisualEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ParticleTypes.CODEC.fieldOf(PARTICLE).forGetter(BurstEntityVisualEffect::particle),
            LevelBasedValue.CODEC.fieldOf(COUNT).forGetter(BurstEntityVisualEffect::count),
            LevelBasedValue.CODEC.fieldOf(SPEED).forGetter(BurstEntityVisualEffect::speed)
    ).apply(instance, BurstEntityVisualEffect::new));

    /**
     * Broadcasts a particle burst around the chest height ($\text{pos} + (0, \text{height}/2, 0)$) of
     * {@code victim} with a $(0.25, 0.5, 0.25)$ spatial spread.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param victim           The target entity intersecting the spell field.
     * @param scalar           The active topological multiplier for {@code victim}.
     */
    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float scalar) {
        int particleCount = (int) (this.count.calculate(enchantmentLevel) * scalar);
        if (particleCount <= 0) return;

        float particleSpeed = this.speed.calculate(enchantmentLevel) * scalar;
        Vec3 pos = victim.position().add(0, victim.getBbHeight() / 2.0, 0);

        level.sendParticles(this.particle, pos.x, pos.y, pos.z, particleCount, 0.25, 0.5, 0.25, particleSpeed);
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() { return CODEC; }
}