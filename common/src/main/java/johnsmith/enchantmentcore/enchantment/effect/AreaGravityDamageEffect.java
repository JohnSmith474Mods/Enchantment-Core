package johnsmith.enchantmentcore.enchantment.effect;

import java.util.List;
import java.util.Set;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record AreaGravityDamageEffect(
        LevelBasedValue strength,
        LevelBasedValue radius,
        LevelBasedValue damage
) implements EnchantmentEntityEffect {

    public static final String STRENGTH = "strength";
    public static final String RADIUS = "radius";
    public static final String DAMAGE = "damage";

    public static final String KEY = "area_gravity_damage";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(STRENGTH, RADIUS, DAMAGE);

    public static final MapCodec<AreaGravityDamageEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(STRENGTH).forGetter(AreaGravityDamageEffect::strength),
            LevelBasedValue.CODEC.fieldOf(RADIUS).forGetter(AreaGravityDamageEffect::radius),
            LevelBasedValue.CODEC.fieldOf(DAMAGE).forGetter(AreaGravityDamageEffect::damage)
    ).apply(instance, AreaGravityDamageEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Vec3 origin) {
        float r = this.radius.calculate(enchantmentLevel);
        float s = this.strength.calculate(enchantmentLevel);
        float d = this.damage.calculate(enchantmentLevel);

        // Construct the cubic Volume of Effect
        AABB voe = target.getBoundingBox().inflate(r);

        LivingEntity owner = context.owner();

        // Exclude the caster/owner from the effect entirely
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, voe, e -> e.isAlive() && e != owner);

        DamageSource source = owner != null ?
                level.damageSources().mobAttack(owner) : level.damageSources().generic();

        for (LivingEntity victim : entities) {
// --- VISUAL & AUDIO EFFECTS (High-Speed Kinetics) ---

            // Find the visual epicenter (chest height of the target)
            Vec3 center = target.position().add(0, target.getBbHeight() / 2.0, 0);

            // Broadcast the warp sound to all nearby players
            level.playSound(
                    null,
                    center.x, center.y, center.z,
                    net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0F,
                    (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F
            );

            int particleCount = (int) (r * r * 10);

            for (int i = 0; i < particleCount; i++) {
                // 1. Generate a uniform random point inside a sphere
                double u = level.random.nextDouble();
                double v = level.random.nextDouble();
                double theta = u * 2.0 * Math.PI;
                double phi = Math.acos(2.0 * v - 1.0);
                double rRand = Math.cbrt(level.random.nextDouble()) * r;

                // 2. Calculate the exact relative offset from the epicenter
                double dx = rRand * Math.sin(phi) * Math.cos(theta);
                double dy = rRand * Math.cos(phi);
                double dz = rRand * Math.sin(phi) * Math.sin(theta);

                if (s > 0) {
                    // IMPLOSION (Pull): Spawn at the outer edge, blast velocity inward.
                    Vec3 spawnPos = new Vec3(center.x + dx, center.y + dy, center.z + dz);
                    Vec3 velocity = new Vec3(-dx, -dy, -dz).normalize();

                    level.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.WITCH,
                            spawnPos.x, spawnPos.y, spawnPos.z,
                            0, // Force velocity mode
                            velocity.x, velocity.y, velocity.z,
                            2.0 // Extreme speed multiplier (~0.15s impact)
                    );
                } else {
                    // EXPLOSION (Push): Spawn at the epicenter, blast velocity outward.
                    Vec3 velocity = new Vec3(dx, dy, dz).normalize();

                    level.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.WITCH,
                            center.x, center.y, center.z,
                            0, // Force velocity mode
                            velocity.x, velocity.y, velocity.z,
                            2.0 // Extreme speed multiplier
                    );
                }
            }

            // --- DAMAGE & EFFECTS ---

            // Apply kinematics to everyone except the epicenter target itself
            if (victim != target) {
                Vec3 dir = target.position().subtract(victim.position());
                double distance = dir.length();

                if (distance > 0.0001D) {
                    dir = dir.normalize();
                } else {
                    dir = Vec3.ZERO;
                }

                // --- SIGMOID PARAMETERS ---
                // In a final mod, you would expose 'k' and 'm' to your JSON codec.
                // For now, we dynamically center the drop-off at half the radius.
                double steepness = 0.7;
                double midpoint = r / 2.0;

                // Calculate the S-Curve scalar (Results in ~1.0 at center, ~0.0 at edge)
                double sigmoidScale = 1.0 / (1.0 + Math.exp(steepness * (distance - midpoint)));

                // Scale the base strength by the sigmoid falloff
                Vec3 impulse = dir.scale(s * sigmoidScale);

                // Overcome vanilla block friction to ensure consistent lateral movement
                double frictionBump = victim.onGround() ? Math.min(0.2D, Math.abs(s * sigmoidScale)) : 0.0D;

                victim.setDeltaMovement(victim.getDeltaMovement().add(impulse.x, impulse.y + frictionBump, impulse.z));
                victim.hurtMarked = true;
            }

            // Apply damage to all entities in the VoE, including the epicenter target
            if (d > 0) {
                victim.hurt(source, d);
            }
        }
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}