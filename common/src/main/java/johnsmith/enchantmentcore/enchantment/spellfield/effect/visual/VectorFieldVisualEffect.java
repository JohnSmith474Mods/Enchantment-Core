package johnsmith.enchantmentcore.enchantment.spellfield.effect.visual;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVisualEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldAxis;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.ProbabilityDistribution;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record VectorFieldVisualEffect(
        ParticleOptions particle,
        LevelBasedValue count,
        LevelBasedValue speed,
        Optional<FieldAxis> axis,
        ProbabilityDistribution distribution
) implements SpellFieldVisualEffect {

    public static final String PARTICLE = "particle";
    public static final String COUNT = "count";
    public static final String SPEED = "speed";
    public static final String AXIS = "axis";
    public static final String DISTRIBUTION = "distribution";

    public static final String KEY = "vector_field";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(COUNT, SPEED);

    public static final MapCodec<VectorFieldVisualEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ParticleTypes.CODEC.fieldOf(PARTICLE).forGetter(VectorFieldVisualEffect::particle),
            LevelBasedValue.CODEC.fieldOf(COUNT).forGetter(VectorFieldVisualEffect::count),
            LevelBasedValue.CODEC.fieldOf(SPEED).forGetter(VectorFieldVisualEffect::speed),
            FieldAxis.CODEC.optionalFieldOf(AXIS).forGetter(VectorFieldVisualEffect::axis),
            ProbabilityDistribution.CODEC.fieldOf(DISTRIBUTION).forGetter(VectorFieldVisualEffect::distribution)
    ).apply(instance, VectorFieldVisualEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Vec3 epicenter, List<GlobalVolume> volumes, List<FieldAxis> activeAxes) {
        float configuredSpeed = this.speed.calculate(enchantmentLevel);
        float polarity = configuredSpeed < 0 ? -1.0F : 1.0F;
        float particleSpeed = Math.abs(configuredSpeed);
        int particleCount = (int) this.count.calculate(enchantmentLevel);

        for (int i = 0; i < particleCount; i++) {
            Vec3 spawnPos = this.distribution.samplePoint(level, enchantmentLevel, target, epicenter, volumes);
            FieldAxis resolvedAxis = this.axis.orElseGet(() -> activeAxes.isEmpty() ? null : activeAxes.get(level.random.nextInt(activeAxes.size())));

            double vx, vy, vz;
            if (resolvedAxis != null) {
                Vec3 velocity = resolvedAxis.getImpulseVector(target, epicenter, spawnPos).normalize().scale(polarity);
                vx = velocity.x;
                vy = velocity.y;
                vz = velocity.z;
            } else {
                vx = (level.random.nextDouble() - 0.5) * 2.0;
                vy = (level.random.nextDouble() - 0.5) * 2.0;
                vz = (level.random.nextDouble() - 0.5) * 2.0;
            }

            level.sendParticles(this.particle, spawnPos.x, spawnPos.y, spawnPos.z, 0, vx, vy, vz, particleSpeed);
        }
    }

    @Override
    public MapCodec<? extends SpellFieldVisualEffect> codec() { return CODEC; }
}