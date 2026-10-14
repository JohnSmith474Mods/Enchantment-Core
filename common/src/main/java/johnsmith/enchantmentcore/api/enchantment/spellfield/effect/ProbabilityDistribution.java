package johnsmith.enchantmentcore.api.enchantment.spellfield.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Rejection-sampling utility for selecting random 3D coordinates within a list of {@link GlobalVolume}s
 * weighted by their internal {@link johnsmith.enchantmentcore.api.enchantment.spellfield.math.Topology} falloff multipliers.
 *
 * @param samplingAttempts The maximum number of Monte Carlo rejection-sampling iterations attempted
 *                         before falling back to a uniform random point inside the volume bounds.
 */
public record ProbabilityDistribution(
        int samplingAttempts
) {
    /** JSON field key for the maximum number of rejection-sampling attempts. */
    public static final String SAMPLING_ATTEMPTS = "sampling_attempts";

    /**
     * Codec for serializing and deserializing {@link ProbabilityDistribution} instances from JSON.
     */
    public static final Codec<ProbabilityDistribution> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf(SAMPLING_ATTEMPTS, 20).forGetter(ProbabilityDistribution::samplingAttempts)
    ).apply(instance, ProbabilityDistribution::new));

    /**
     * Samples a random world-space coordinate from {@code volumes} weighted by each volume's topological multiplier.
     *
     * @param level            The active server level providing the random source.
     * @param enchantmentLevel The active level of the enchantment.
     * @param spatialReference The reference entity anchoring the spell field.
     * @param epicenter        The fallback epicenter returned if {@code volumes} is empty.
     * @param volumes          The list of evaluated {@link GlobalVolume}s to sample from.
     * @return A sampled world-space {@link Vec3} coordinate.
     */
    public Vec3 samplePoint(ServerLevel level, int enchantmentLevel, Entity spatialReference, Vec3 epicenter, List<GlobalVolume> volumes) {
        if (volumes.isEmpty()) return epicenter;

        for (int i = 0; i < this.samplingAttempts; i++) {
            GlobalVolume vol = volumes.get(level.random.nextInt(volumes.size()));
            AABB box = vol.bounds();

            double x = Mth.lerp(level.random.nextDouble(), box.minX, box.maxX);
            double y = Mth.lerp(level.random.nextDouble(), box.minY, box.maxY);
            double z = Mth.lerp(level.random.nextDouble(), box.minZ, box.maxZ);
            Vec3 candidate = new Vec3(x, y, z);

            if (vol.topology() == null) return candidate;

            float prob = vol.topology().evaluateMultiplier(enchantmentLevel, spatialReference, vol.volumeCenter(), candidate);
            if (prob >= 1.0F || level.random.nextFloat() <= prob) {
                return candidate;
            }
        }

        return getUniformPoint(volumes, level.random);
    }

    /**
     * Selects a uniformly distributed random point inside the bounding box of a randomly chosen {@link GlobalVolume}.
     *
     * @param volumes The list of candidate {@link GlobalVolume}s.
     * @param random  The random number generator.
     * @return A uniformly sampled world-space {@link Vec3}, or {@link Vec3#ZERO} if {@code volumes} is empty.
     */
    public static Vec3 getUniformPoint(List<GlobalVolume> volumes, RandomSource random) {
        if (volumes.isEmpty()) return Vec3.ZERO;
        GlobalVolume vol = volumes.get(random.nextInt(volumes.size()));
        AABB box = vol.bounds();
        double x = Mth.lerp(random.nextDouble(), box.minX, box.maxX);
        double y = Mth.lerp(random.nextDouble(), box.minY, box.maxY);
        double z = Mth.lerp(random.nextDouble(), box.minZ, box.maxZ);
        return new Vec3(x, y, z);
    }
}