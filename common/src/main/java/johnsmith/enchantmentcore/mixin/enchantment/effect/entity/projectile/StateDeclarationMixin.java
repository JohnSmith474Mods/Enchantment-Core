package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.projectile;

import johnsmith.enchantmentcore.api.entity.accessor.ProjectileStateAccessor;

import net.minecraft.world.entity.projectile.Projectile;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin injecting ProjectileStateAccessor state fields into the base Projectile class.
 * Allocates memory footprint for custom projectile physics and behavioral parameters.
 * Allows independent, isolated state tracking for every instantiated projectile.
 */
@Mixin(Projectile.class)
public abstract class StateDeclarationMixin implements ProjectileStateAccessor {

    // --- PHYSICS MODIFIERS ---
    @Unique private Double enchantment_core$dragMultiplier = null;

    // --- HOMING PARAMETERS ---
    @Unique private Double enchantment_core$homingStrength = null;
    @Unique private Boolean enchantment_core$homingPrioritizesHead = null;
    @Unique private Double enchantment_core$homingArmingDistance = null;
    @Unique private Double enchantment_core$homingMinDistance = null;
    @Unique private Double enchantment_core$homingMaxDistance = null;
    @Unique private Double enchantment_core$homingFov = null;
    @Unique private Double enchantment_core$homingTurnRate = null;

    // --- RICOCHET PARAMETERS ---
    @Unique private Integer enchantment_core$bouncesRemaining = null;
    @Unique private Double enchantment_core$ricochetRetention = null;

    // --- MAGNETIC PARAMETERS ---
    @Unique private Double enchantment_core$magneticStrength = null;
    @Unique private Boolean enchantment_core$magneticPrioritizesHead = null;
    @Unique private Double enchantment_core$magneticArmingDistance = null;
    @Unique private Double enchantment_core$magneticSearchRadius = null;

    // --- SHRAPNEL PARAMETERS ---
    @Unique private Integer enchantment_core$shrapnelGenerations = null;
    @Unique private Integer enchantment_core$shrapnelAmount = null;
    @Unique private Double enchantment_core$shrapnelSpread = null;
    @Unique private Double enchantment_core$shrapnelVelocityRetention = null;
    @Unique private Double enchantment_core$shrapnelDamageRetention = null;
    @Unique private Boolean enchantment_core$shrapnelTriggerBlock = null;
    @Unique private Boolean enchantment_core$shrapnelTriggerEntity = null;

    // --- STATE LOCK ---
    @Unique private boolean enchantment_core$stateCalculated = false;

    // --- WRITE ACCESSORS ---
    @Override public void enchantment_core$setDragMultiplier(double multiplier) { this.enchantment_core$dragMultiplier = multiplier; }

    @Override public void enchantment_core$setHomingStrength(double strength) { this.enchantment_core$homingStrength = strength; }
    @Override public void enchantment_core$setHomingPrioritizesHead(boolean head) { this.enchantment_core$homingPrioritizesHead = head; }
    @Override public void enchantment_core$setHomingArmingDistance(double distance) { this.enchantment_core$homingArmingDistance = distance; }
    @Override public void enchantment_core$setHomingMinDistance(double distance) { this.enchantment_core$homingMinDistance = distance; }
    @Override public void enchantment_core$setHomingMaxDistance(double distance) { this.enchantment_core$homingMaxDistance = distance; }
    @Override public void enchantment_core$setHomingFov(double fov) { this.enchantment_core$homingFov = fov; }
    @Override public void enchantment_core$setHomingTurnRate(double turnRate) { this.enchantment_core$homingTurnRate = turnRate; }

    @Override public void enchantment_core$setBouncesRemaining(int bounces) { this.enchantment_core$bouncesRemaining = bounces; }
    @Override public void enchantment_core$setRicochetRetention(double retention) { this.enchantment_core$ricochetRetention = retention; }

    @Override public void enchantment_core$setMagneticStrength(double strength) { this.enchantment_core$magneticStrength = strength; }
    @Override public void enchantment_core$setMagneticPrioritizesHead(boolean head) { this.enchantment_core$magneticPrioritizesHead = head; }
    @Override public void enchantment_core$setMagneticArmingDistance(double distance) { this.enchantment_core$magneticArmingDistance = distance; }
    @Override public void enchantment_core$setMagneticSearchRadius(double radius) { this.enchantment_core$magneticSearchRadius = radius; }

    @Override public void enchantment_core$setShrapnelGenerations(int gens) { this.enchantment_core$shrapnelGenerations = gens; }
    @Override public void enchantment_core$setShrapnelAmount(int amount) { this.enchantment_core$shrapnelAmount = amount; }
    @Override public void enchantment_core$setShrapnelSpread(double spread) { this.enchantment_core$shrapnelSpread = spread; }
    @Override public void enchantment_core$setShrapnelVelocityRetention(double retention) { this.enchantment_core$shrapnelVelocityRetention = retention; }
    @Override public void enchantment_core$setShrapnelDamageRetention(double retention) { this.enchantment_core$shrapnelDamageRetention = retention; }
    @Override public void enchantment_core$setShrapnelTriggerBlock(boolean trigger) { this.enchantment_core$shrapnelTriggerBlock = trigger; }
    @Override public void enchantment_core$setShrapnelTriggerEntity(boolean trigger) { this.enchantment_core$shrapnelTriggerEntity = trigger; }
    @Override public void enchantment_core$setCalculated(boolean calculated) { this.enchantment_core$stateCalculated = calculated; }

    // --- READ ACCESSORS (With Non-Null Defaults) ---
    @Override public double enchantment_core$getDragMultiplier() { return this.enchantment_core$dragMultiplier != null ? this.enchantment_core$dragMultiplier : 1.0; }

    @Override public double enchantment_core$getHomingStrength() { return this.enchantment_core$homingStrength != null ? this.enchantment_core$homingStrength : 0.0; }
    @Override public boolean enchantment_core$getHomingPrioritizesHead() { return this.enchantment_core$homingPrioritizesHead != null ? this.enchantment_core$homingPrioritizesHead : false; }
    @Override public double enchantment_core$getHomingArmingDistance() { return this.enchantment_core$homingArmingDistance != null ? this.enchantment_core$homingArmingDistance : 0.0; }
    @Override public double enchantment_core$getHomingMinDistance() { return this.enchantment_core$homingMinDistance != null ? this.enchantment_core$homingMinDistance : 0.0; }
    @Override public double enchantment_core$getHomingMaxDistance() { return this.enchantment_core$homingMaxDistance != null ? this.enchantment_core$homingMaxDistance : 64.0;}
    @Override public double enchantment_core$getHomingFov() { return this.enchantment_core$homingFov != null ? this.enchantment_core$homingFov : 360.0; }
    @Override public double enchantment_core$getHomingTurnRate() { return this.enchantment_core$homingTurnRate != null ? this.enchantment_core$homingTurnRate : 360.0; }

    @Override public int enchantment_core$getBouncesRemaining() { return this.enchantment_core$bouncesRemaining != null ? this.enchantment_core$bouncesRemaining : 0; }
    @Override public double enchantment_core$getRicochetRetention() { return this.enchantment_core$ricochetRetention != null ? this.enchantment_core$ricochetRetention : 0.8; }

    @Override public double enchantment_core$getMagneticStrength() { return this.enchantment_core$magneticStrength != null ? this.enchantment_core$magneticStrength : 0.0; }
    @Override public boolean enchantment_core$getMagneticPrioritizesHead() { return this.enchantment_core$magneticPrioritizesHead != null ? this.enchantment_core$magneticPrioritizesHead : false; }
    @Override public double enchantment_core$getMagneticArmingDistance() { return this.enchantment_core$magneticArmingDistance != null ? this.enchantment_core$magneticArmingDistance : 0.0; }
    @Override public double enchantment_core$getMagneticSearchRadius() { return this.enchantment_core$magneticSearchRadius != null ? this.enchantment_core$magneticSearchRadius : 16.0; }

    @Override public int enchantment_core$getShrapnelGenerations() { return this.enchantment_core$shrapnelGenerations != null ? this.enchantment_core$shrapnelGenerations : 0; }
    @Override public int enchantment_core$getShrapnelAmount() { return this.enchantment_core$shrapnelAmount != null ? this.enchantment_core$shrapnelAmount : 0; }
    @Override public double enchantment_core$getShrapnelSpread() { return this.enchantment_core$shrapnelSpread != null ? this.enchantment_core$shrapnelSpread : 0.0; }
    @Override public double enchantment_core$getShrapnelVelocityRetention() { return this.enchantment_core$shrapnelVelocityRetention != null ? this.enchantment_core$shrapnelVelocityRetention : 0.0; }
    @Override public double enchantment_core$getShrapnelDamageRetention() { return this.enchantment_core$shrapnelDamageRetention != null ? this.enchantment_core$shrapnelDamageRetention : 0.0; }
    @Override public boolean enchantment_core$getShrapnelTriggerBlock() { return this.enchantment_core$shrapnelTriggerBlock != null ? this.enchantment_core$shrapnelTriggerBlock : false; }
    @Override public boolean enchantment_core$getShrapnelTriggerEntity() { return this.enchantment_core$shrapnelTriggerEntity != null ? this.enchantment_core$shrapnelTriggerEntity : false; }
    @Override public boolean enchantment_core$isCalculated() { return this.enchantment_core$stateCalculated; }
}