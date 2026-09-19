package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.living;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.api.enchantment.effect.FluidWalkerDefinition;
import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.api.entity.accessor.LivingEntityStateAccessor;
import johnsmith.enchantmentcore.api.entity.accessor.StreakStateAccessor;
import johnsmith.enchantmentcore.enchantment.effect.*;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin implementing stateful passive behaviors on {@link LivingEntity}.
 * Manages dynamic multi-jumping, surface climbing, fluid-walking tracking, buoyancy, and streak timeouts.
 */
@Mixin(LivingEntity.class)
public abstract class PassiveEffectsMixin extends Entity implements LivingEntityStateAccessor, StreakStateAccessor {

    @Shadow protected boolean jumping;
    @Shadow protected abstract void jumpFromGround();

    @Unique private final List<FluidWalkerDefinition> enchantment_core$activeFluidWalkers = new ArrayList<>();
    @Unique private final List<Integer> enchantment_core$activeFluidWalkerLevels = new ArrayList<>();
    @Unique private boolean enchantment_core$hasBuoyancy = false;

    @Unique private int enchantment_core$maxMultiJumps = 0;
    @Unique private int enchantment_core$midairJumpsPerformed = 0;
    @Unique private boolean enchantment_core$wasJumping = false;
    @Unique private boolean enchantment_core$allowElytraMultiJump = false;
    @Unique private boolean enchantment_core$isMultiJumping = false;
    @Unique private int enchantment_core$jumpCooldown = 0;

    @Unique private final List<ClimbingEffect> enchantment_core$activeClimbers = new ArrayList<>();
    @Unique private final List<Integer> enchantment_core$activeClimberLevels = new ArrayList<>();

    @Unique private String enchantment_core$streakTargetId = "";
    @Unique private int enchantment_core$streakCount = 0;
    @Unique private int enchantment_core$streakTimeout = 0;

    public PassiveEffectsMixin() {
        super(null, null);
    }

    @Override
    public List<FluidWalkerDefinition> enchantment_core$getActiveFluidWalkers() {
        return this.enchantment_core$activeFluidWalkers;
    }

    @Override
    public int enchantment_core$getStreakCount(String targetId) {
        return this.enchantment_core$streakTargetId.equals(targetId) ? this.enchantment_core$streakCount : 0;
    }

    @Override
    public void enchantment_core$incrementStreak(String targetId, int maxStreak, int timeoutTicks) {
        if (this.enchantment_core$streakTargetId.equals(targetId)) {
            this.enchantment_core$streakCount = Math.min(this.enchantment_core$streakCount + 1, maxStreak);
        } else {
            this.enchantment_core$streakTargetId = targetId;
            this.enchantment_core$streakCount = 1;
        }
        this.enchantment_core$streakTimeout = timeoutTicks;
    }

    /**
     * Evaluates whether the entity is colliding with a block valid for climbing under active enchantments.
     */
    @Unique
    private ClimbingEffect enchantment_core$getValidClimbingEffect() {
        if (this.enchantment_core$activeClimbers.isEmpty()) return null;

        LivingEntity entity = (LivingEntity) (Object) this;
        AABB wallBox = entity.getBoundingBox().inflate(0.05D, 0.0D, 0.05D); // Slightly inflate the hitbox horizontally to detect wall proximity.

        for (ClimbingEffect effect : this.enchantment_core$activeClimbers) {
            Optional<HolderSet<Block>> allowedOpt = effect.allowedBlocks();

            boolean matches = entity.level().getBlockStates(wallBox).anyMatch(state -> {
                if (state.isAir()) return false;
                if (allowedOpt.isPresent()) {
                    return state.is(allowedOpt.get());
                } else {
                    return state.blocksMotion();
                }
            });

            if (matches) return effect;
        }
        return null;
    }

    /**
     * Evaluates continuous passive enchantments every entity tick.
     * Refreshes active arrays and handles countdown timeouts.
     */
    @Inject(method = "baseTick", at = @At("HEAD"))
    private void enchantment_core$evaluateTickStates(CallbackInfo ci) {
        // Decrement and expire active streak tracking.
        if (this.enchantment_core$streakTimeout > 0) {
            this.enchantment_core$streakTimeout--;
            if (this.enchantment_core$streakTimeout == 0) {
                this.enchantment_core$streakTargetId = "";
                this.enchantment_core$streakCount = 0;
            }
        }

        // Flush per-tick passive containers.
        this.enchantment_core$activeFluidWalkers.clear();
        this.enchantment_core$activeFluidWalkerLevels.clear();
        this.enchantment_core$hasBuoyancy = false;
        this.enchantment_core$maxMultiJumps = 0;
        this.enchantment_core$allowElytraMultiJump = false;
        this.enchantment_core$activeClimbers.clear();
        this.enchantment_core$activeClimberLevels.clear();

        LivingEntity entity = (LivingEntity) (Object) this;
        boolean isClient = entity.level().isClientSide();
        LootContext lootContext = null;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipment = entity.getItemBySlot(slot);
            if (equipment.isEmpty()) continue;

            ItemEnchantments enchantments = equipment.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (enchantments.isEmpty()) continue;

            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                Holder<Enchantment> ench = entry.getKey();
                if (!ench.value().matchingSlot(slot)) continue;

                int level = entry.getIntValue();

                if (!isClient && lootContext == null) {
                    LootParams params = new LootParams.Builder((ServerLevel) entity.level())
                            .withParameter(LootContextParams.THIS_ENTITY, entity)
                            .withParameter(LootContextParams.ORIGIN, entity.position())
                            .withParameter(LootContextParams.ENCHANTMENT_LEVEL, level)
                            .create(LootContextParamSets.ENCHANTED_ENTITY);
                    lootContext = new LootContext.Builder(params).create(Optional.empty());
                }

                List<ConditionalEffect<FluidWalkerEffect>> fwEffects = ench.value().effects().get(EnchantmentEffectComponentRegistry.FLUID_WALKER.get());
                if (fwEffects != null) {
                    for (ConditionalEffect<FluidWalkerEffect> cond : fwEffects) {
                        if (isClient || cond.matches(lootContext)) {
                            this.enchantment_core$activeFluidWalkers.add(cond.effect());
                            this.enchantment_core$activeFluidWalkerLevels.add(level);
                        }
                    }
                }

                if (!this.enchantment_core$hasBuoyancy) {
                    List<ConditionalEffect<BuoyancyEffect>> bEffects = ench.value().effects().get(EnchantmentEffectComponentRegistry.BUOYANCY.get());
                    if (bEffects != null) {
                        for (ConditionalEffect<BuoyancyEffect> cond : bEffects) {
                            if (isClient || cond.matches(lootContext)) {
                                this.enchantment_core$hasBuoyancy = true;
                                break;
                            }
                        }
                    }
                }

                List<ConditionalEffect<MultiJumpEffect>> mjEffects = ench.value().effects().get(EnchantmentEffectComponentRegistry.MULTI_JUMP.get());
                if (mjEffects != null) {
                    for (ConditionalEffect<MultiJumpEffect> cond : mjEffects) {
                        if (isClient || cond.matches(lootContext)) {
                            int jumps = (int) cond.effect().jumps().process(level, entity.getRandom(), 0.0F);
                            if (jumps > this.enchantment_core$maxMultiJumps) {
                                this.enchantment_core$maxMultiJumps = jumps;
                            }
                            if (cond.effect().allowElytra()) {
                                this.enchantment_core$allowElytraMultiJump = true;
                            }
                        }
                    }
                }

                List<ConditionalEffect<ClimbingEffect>> cEffects = ench.value().effects().get(EnchantmentEffectComponentRegistry.CLIMBING.get());
                if (cEffects != null) {
                    for (ConditionalEffect<ClimbingEffect> cond : cEffects) {
                        if (isClient || cond.matches(lootContext)) {
                            this.enchantment_core$activeClimbers.add(cond.effect());
                            this.enchantment_core$activeClimberLevels.add(level);
                        }
                    }
                }
            }
        }
    }

    /**
     * Resets mid-air jump limits whenever a standard jump executes from solid footing.
     */
    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void enchantment_core$onVanillaJump(CallbackInfo ci) {
        if (!this.enchantment_core$isMultiJumping) {
            this.enchantment_core$midairJumpsPerformed = 0;
            this.enchantment_core$jumpCooldown = 5;
        }
    }

    /**
     * Evaluates jump key inputs in mid-air to process multi-jump mechanics.
     */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void enchantment_core$evaluateMultiJump(CallbackInfo ci) {
        if (this.enchantment_core$jumpCooldown > 0) {
            this.enchantment_core$jumpCooldown--;
        }

        if (this.onGround() || this.isInWater() || this.isInLava()) {
            this.enchantment_core$midairJumpsPerformed = 0;
        } else if (this.jumping && !this.enchantment_core$wasJumping && !this.isPassenger() && this.enchantment_core$jumpCooldown == 0) {
            // Check flight exclusions.
            if (((LivingEntity) (Object) this).isFallFlying() && !this.enchantment_core$allowElytraMultiJump) {
                return;
            } else if (this.enchantment_core$midairJumpsPerformed < this.enchantment_core$maxMultiJumps) {
                this.enchantment_core$isMultiJumping = true;
                this.jumpFromGround(); // Re-trigger vanilla jump impulse logic.
                this.enchantment_core$isMultiJumping = false;
                this.enchantment_core$midairJumpsPerformed++;
                this.enchantment_core$jumpCooldown = 5; // Enforce minimum tick delay between jumps.
            }
        }
        this.enchantment_core$wasJumping = this.jumping;
    }

    /**
     * Overrides vanilla ladder/vine detection if the entity has a valid climbing enchantment against a wall.
     */
    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void enchantment_core$applySpiderClimbing(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && this.enchantment_core$getValidClimbingEffect() != null) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Modifies the movement vector applied during travel calculations for climbing and vertical suspension.
     */
    @ModifyArg(
            method = "handleRelativeFrictionAndCalculateMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
            ),
            index = 1
    )
    private Vec3 enchantment_core$applyClimbingModifications(Vec3 originalDelta) {
        if (this.isInWater() || this.isInLava() || ((LivingEntity) (Object) this).isFallFlying()) return originalDelta;

        ClimbingEffect effect = this.enchantment_core$getValidClimbingEffect();
        if (effect != null) {
            double dy = originalDelta.y;
            int effectIndex = this.enchantment_core$activeClimbers.indexOf(effect);
            float speedMult = effect.speed().calculate(this.enchantment_core$activeClimberLevels.get(effectIndex));

            // Hold vertical position completely when crouching mid-air against a wall.
            if (effect.holdOnCrouch() && this.isShiftKeyDown() && !this.onGround()) {
                dy = 0.0D;
            } else if (this.horizontalCollision) {
                // Apply upward velocity when driving into the wall.
                double targetSpeed = 0.2D * speedMult;
                if (dy < targetSpeed) {
                    dy = targetSpeed;
                }
            }

            if (dy != originalDelta.y) {
                Vec3 newDelta = new Vec3(originalDelta.x, dy, originalDelta.z);
                this.setDeltaMovement(newDelta);
                return newDelta;
            }
        }
        return originalDelta;
    }

    /**
     * Applies speed retention multipliers when walking over solid fluid surfaces.
     */
    @Inject(method = "getBlockSpeedFactor", at = @At("RETURN"), cancellable = true)
    private void enchantment_core$modifyFluidWalkSpeed(CallbackInfoReturnable<Float> cir) {
        if (this.enchantment_core$activeFluidWalkers.isEmpty()) return;

        LivingEntity entity = (LivingEntity) (Object) this;
        FluidState fluidState = entity.level().getFluidState(entity.getBlockPosBelowThatAffectsMyMovement());
        if (fluidState.isEmpty()) return;

        float aggregatedRetention = 1.0F;
        boolean matched = false;

        for (int i = 0; i < this.enchantment_core$activeFluidWalkers.size(); i++) {
            FluidWalkerDefinition effect = this.enchantment_core$activeFluidWalkers.get(i);
            if (fluidState.is(effect.allowedFluids())) {
                aggregatedRetention *= effect.speedRetention().calculate(this.enchantment_core$activeFluidWalkerLevels.get(i));
                matched = true;
            }
        }

        if (matched) {
            cir.setReturnValue(cir.getReturnValueF() * aggregatedRetention);
        }
    }

    /**
     * Prevents sinking while submerged in fluid if buoyancy is active, keeping the entity afloat.
     */
    @Inject(method = "travel", at = @At("TAIL"))
    private void enchantment_core$applyBuoyancyEquilibrium(Vec3 travelVector, CallbackInfo ci) {
        if (!this.enchantment_core$hasBuoyancy) return;

        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity.isInWater() || entity.isInLava())) return;
        // Suppress buoyancy when deliberately descending.
        if (entity.isCrouching() || travelVector.y < 0.0) return;
        if (travelVector.z > 0.0 && entity.getXRot() > 45.0F) return; // Diving pitch threshold.

        Vec3 delta = entity.getDeltaMovement();
        if (delta.y < 0.0) {
            entity.setDeltaMovement(delta.x, 0.0, delta.z); // Clamp negative vertical drift.
        }
    }
}