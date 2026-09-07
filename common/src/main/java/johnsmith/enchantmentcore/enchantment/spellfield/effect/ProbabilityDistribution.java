package johnsmith.enchantmentcore.enchantment.spellfield.effect;

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

public record ProbabilityDistribution(
        int samplingAttempts
) {
    public static final String SAMPLING_ATTEMPTS = "sampling_attempts";

    public static final Codec<ProbabilityDistribution> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf(SAMPLING_ATTEMPTS, 20).forGetter(ProbabilityDistribution::samplingAttempts)
    ).apply(instance, ProbabilityDistribution::new));

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