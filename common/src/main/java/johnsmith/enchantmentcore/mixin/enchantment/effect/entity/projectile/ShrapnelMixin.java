package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.projectile;

import com.mojang.logging.LogUtils;

import java.util.Comparator;
import java.util.List;

import johnsmith.enchantmentcore.api.entity.accessor.ProjectileStateAccessor;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin responsible for generating sub-munitions upon projectile impact.
 * Handles copying NBT data and distributing shrapnel dynamically based on the impact type.
 */
@Mixin(Projectile.class)
public abstract class ShrapnelMixin {

    @Unique
    private static final Logger enchantment_core$LOGGER = LogUtils.getLogger();

    /**
     * Intercepts the generalized hit resolution event for all projectiles.
     * Spawns identical clone entities and distributes them via radial iteration or direct targeting.
     */
    @Inject(method = "onHit", at = @At("HEAD"))
    private void enchantment_core$triggerShrapnel(HitResult hitResult, CallbackInfo ci) {
        if (hitResult.getType() == HitResult.Type.MISS) return;

        Projectile projectile = (Projectile) (Object) this;
        ProjectileStateAccessor state = (ProjectileStateAccessor) projectile;

        int generations = state.enchantment_core$getShrapnelGenerations();
        if (generations <= 0) return;

        if (hitResult.getType() == HitResult.Type.BLOCK && !state.enchantment_core$getShrapnelTriggerBlock()) return;

        Entity primaryTarget = null;
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            if (!state.enchantment_core$getShrapnelTriggerEntity()) return;
            primaryTarget = ((EntityHitResult) hitResult).getEntity();
            if (primaryTarget == projectile.getOwner()) return;
        }

        if (!(projectile.level() instanceof ServerLevel serverLevel)) return;

        int amount = state.enchantment_core$getShrapnelAmount();
        double spread = state.enchantment_core$getShrapnelSpread();
        double velRetention = state.enchantment_core$getShrapnelVelocityRetention();
        double dmgRetention = state.enchantment_core$getShrapnelDamageRetention();

        Vec3 originalVelocity = projectile.getDeltaMovement();
        if (originalVelocity.lengthSqr() < 0.001) return;

        double currentSpeed = originalVelocity.length();
        double newSpeed = currentSpeed * velRetention;

        CompoundTag sourceTag;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(projectile.problemPath(), enchantment_core$LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, projectile.registryAccess());
            projectile.saveWithoutId(output);
            sourceTag = (CompoundTag) output.buildResult();
        }

        sourceTag.remove("UUID");
        state.enchantment_core$setShrapnelGenerations(0);

        Vec3 blockBounceNormal = null;
        double blockSpawnX = 0, blockSpawnY = 0, blockSpawnZ = 0;
        List<LivingEntity> secondaryTargets = null;
        double entityRadius = 0;
        double entityEyeY = 0;

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            Vec3 impactPos = hitResult.getLocation();
            Direction.Axis axis = ((BlockHitResult) hitResult).getDirection().getAxis();

            double vx = originalVelocity.x;
            double vy = originalVelocity.y;
            double vz = originalVelocity.z;

            if (axis == Direction.Axis.X) vx = -vx;
            else if (axis == Direction.Axis.Y) vy = -vy;
            else if (axis == Direction.Axis.Z) vz = -vz;

            blockBounceNormal = new Vec3(vx, vy, vz).normalize();

            blockSpawnX = impactPos.x + blockBounceNormal.x * 0.2;
            blockSpawnY = impactPos.y + blockBounceNormal.y * 0.2;
            blockSpawnZ = impactPos.z + blockBounceNormal.z * 0.2;

        } else if (primaryTarget != null) {
            entityRadius = primaryTarget.getBbWidth() / 2.0 + 0.2;
            entityEyeY = primaryTarget.getY() + primaryTarget.getEyeHeight();
            AABB searchBox = primaryTarget.getBoundingBox().inflate(16.0);

            Entity finalPrimaryTarget = primaryTarget;
            secondaryTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != projectile.getOwner() && e != finalPrimaryTarget && e.isAlive());
            secondaryTargets.sort(Comparator.comparingDouble(a -> a.distanceToSqr(finalPrimaryTarget)));
        }

        for (int i = 0; i < amount; i++) {
            Entity clone = projectile.getType().create(serverLevel, EntitySpawnReason.TRIGGERED);
            if (clone instanceof Projectile child) {
                try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(child.problemPath(), enchantment_core$LOGGER)) {
                    child.load(TagValueInput.create(reporter, child.registryAccess(), sourceTag));
                }

                ProjectileStateAccessor childState = (ProjectileStateAccessor) child;

                childState.enchantment_core$setCalculated(true);
                childState.enchantment_core$setShrapnelGenerations(generations - 1);
                childState.enchantment_core$setShrapnelAmount(amount);
                childState.enchantment_core$setShrapnelSpread(spread);
                childState.enchantment_core$setShrapnelVelocityRetention(velRetention);
                childState.enchantment_core$setShrapnelDamageRetention(dmgRetention);
                childState.enchantment_core$setShrapnelTriggerBlock(state.enchantment_core$getShrapnelTriggerBlock());
                childState.enchantment_core$setShrapnelTriggerEntity(state.enchantment_core$getShrapnelTriggerEntity());

                if (child instanceof AbstractArrow arrow && projectile instanceof AbstractArrow sourceArrow) {
                    arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    double parentDamage = ((AbstractArrowAccessor) sourceArrow).enchantment_core$getBaseDamage();
                    arrow.setBaseDamage(parentDamage * dmgRetention);
                }

                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    child.setPos(blockSpawnX, blockSpawnY, blockSpawnZ);
                    child.shoot(blockBounceNormal.x, blockBounceNormal.y, blockBounceNormal.z, (float) newSpeed, (float) spread);
                } else if (primaryTarget != null) {
                    Vec3 horizontalDir;
                    Vec3 aimDir;
                    float actualSpread = 0.0f;

                    if (i < secondaryTargets.size()) {
                        LivingEntity secTarget = secondaryTargets.get(i);
                        Vec3 targetPos = secTarget.getBoundingBox().getCenter();
                        horizontalDir = new Vec3(targetPos.x - primaryTarget.getX(), 0, targetPos.z - primaryTarget.getZ()).normalize();

                        if (horizontalDir.lengthSqr() < 1e-5) {
                            horizontalDir = new Vec3(1, 0, 0);
                        }

                        double sx = primaryTarget.getX() + horizontalDir.x * entityRadius;
                        double sy = entityEyeY;
                        double sz = primaryTarget.getZ() + horizontalDir.z * entityRadius;

                        child.setPos(sx, sy, sz);
                        aimDir = targetPos.subtract(sx, sy, sz).normalize();
                    } else {
                        double angle = (2 * Math.PI * i) / amount;
                        horizontalDir = new Vec3(Math.cos(angle), 0, Math.sin(angle)).normalize();

                        double sx = primaryTarget.getX() + horizontalDir.x * entityRadius;
                        double sy = entityEyeY;
                        double sz = primaryTarget.getZ() + horizontalDir.z * entityRadius;

                        child.setPos(sx, sy, sz);
                        aimDir = horizontalDir;
                        actualSpread = (float) spread;
                    }
                    child.shoot(aimDir.x, aimDir.y, aimDir.z, (float) newSpeed, actualSpread);
                }
                serverLevel.addFreshEntity(child);
            }
        }
    }
}