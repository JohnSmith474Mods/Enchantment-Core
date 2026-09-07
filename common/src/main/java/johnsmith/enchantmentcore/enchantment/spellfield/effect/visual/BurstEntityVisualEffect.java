package johnsmith.enchantmentcore.enchantment.spellfield.effect.visual;

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

public record BurstEntityVisualEffect(
        ParticleOptions particle,
        LevelBasedValue count,
        LevelBasedValue speed
) implements SpellFieldEntityEffect {

    public static final String PARTICLE = "particle";
    public static final String COUNT = "count";
    public static final String SPEED = "speed";

    public static final String KEY = "burst_entity";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(COUNT, SPEED);

    public static final MapCodec<BurstEntityVisualEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ParticleTypes.CODEC.fieldOf(PARTICLE).forGetter(BurstEntityVisualEffect::particle),
            LevelBasedValue.CODEC.fieldOf(COUNT).forGetter(BurstEntityVisualEffect::count),
            LevelBasedValue.CODEC.fieldOf(SPEED).forGetter(BurstEntityVisualEffect::speed)
    ).apply(instance, BurstEntityVisualEffect::new));

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