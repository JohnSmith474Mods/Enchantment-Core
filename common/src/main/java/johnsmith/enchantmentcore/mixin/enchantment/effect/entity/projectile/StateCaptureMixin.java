package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.projectile;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.api.entity.accessor.ProjectileStateAccessor;
import johnsmith.enchantmentcore.enchantment.effect.projectile.HomingProjectileEffect;
import johnsmith.enchantmentcore.enchantment.effect.projectile.MagneticProjectileEffect;
import johnsmith.enchantmentcore.enchantment.effect.projectile.RicochetProjectileEffect;
import johnsmith.enchantmentcore.enchantment.effect.projectile.ShrapnelProjectileEffect;
import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import org.apache.commons.lang3.mutable.MutableFloat;

import org.jetbrains.annotations.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin responsible for evaluating and caching projectile enchantment mechanics.
 * Executes a lazy initialization pass at the start of the projectile's lifespan to prevent
 * excessive per-tick LootContext evaluations.
 */
@Mixin(Projectile.class)
public abstract class StateCaptureMixin {

    @Shadow @Nullable public abstract Entity getOwner();

    @Unique private int enchantment_core$captureRetries = 0;

    /**
     * Intercepts the head of the tick execution loop to process data-driven weapon conditions.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void enchantment_core$lazyEvaluateAeroState(CallbackInfo ci) {
        ProjectileStateAccessor state = (ProjectileStateAccessor) this;

        // Skip execution if state compilation was already completed successfully.
        if (state.enchantment_core$isCalculated()) return;

        Entity owner = this.getOwner();
        if (owner == null) {
            // Delay calculation for a limited number of ticks if the network/owner synchronization is lagging.
            if (this.enchantment_core$captureRetries++ < 10) return;
            state.enchantment_core$setCalculated(true);
            return;
        }

        // Lock further recalculation cycles.
        state.enchantment_core$setCalculated(true);

        if (owner instanceof LivingEntity living) {
            ItemStack weapon = living.getUseItem();
            if (weapon.isEmpty()) weapon = living.getMainHandItem();

            if (!weapon.isEmpty()) {
                ItemEnchantments enchantments = weapon.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

                if (!enchantments.isEmpty()) {
                    MutableFloat modifiedGravity = new MutableFloat(1.0f);
                    MutableFloat modifiedDrag = new MutableFloat(1.0f);

                    MutableFloat homingStrength = new MutableFloat(0.0f);
                    boolean prioritizeHead = false;
                    double homingArmingDist = 0.0;
                    double homingMinDist = 0.0;
                    double homingMaxDist = 0.0;
                    double homingFov = 0.0;
                    double homingTurnRate = 0.0;

                    int ricochetBounces = 0;
                    double ricochetRetention = 0.0;

                    MutableFloat magneticStrength = new MutableFloat(0.0f);
                    boolean magneticPrioritizeHead = false;
                    double magneticArmingDist = 0.0;
                    double magneticSearchRadius = 0.0;

                    boolean isServer = living.level() instanceof ServerLevel;
                    LootContext lootContext = null;

                    // Build LootContext solely on the server environment.
                    if (isServer) {
                        LootParams params = new LootParams.Builder((ServerLevel) living.level())
                                .withParameter(LootContextParams.THIS_ENTITY, living)
                                .withParameter(LootContextParams.ENCHANTMENT_LEVEL, 1) // Base context uses Level 1 as placeholder
                                .withParameter(LootContextParams.ORIGIN, living.position())
                                .create(LootContextParamSets.ENCHANTED_ENTITY);
                        lootContext = new LootContext.Builder(params).create(Optional.empty());
                    }

                    // Iterate components to map modifications.
                    for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                        Holder<Enchantment> enchantment = entry.getKey();
                        int level = entry.getIntValue();

                        // Parse Gravity.
                        List<ConditionalEffect<EnchantmentValueEffect>> gravityEffects = enchantment.value().effects().get(EnchantmentEffectComponentRegistry.GRAVITY_MODIFIER);
                        if (gravityEffects != null) {
                            for (ConditionalEffect<EnchantmentValueEffect> conditional : gravityEffects) {
                                if (!isServer || conditional.matches(lootContext)) {
                                    modifiedGravity.setValue(conditional.effect().process(level, living.getRandom(), modifiedGravity.getValue()));
                                }
                            }
                        }

                        // Parse Drag.
                        List<ConditionalEffect<EnchantmentValueEffect>> dragEffects = enchantment.value().effects().get(EnchantmentEffectComponentRegistry.PROJECTILE_DRAG);
                        if (dragEffects != null) {
                            for (ConditionalEffect<EnchantmentValueEffect> conditional : dragEffects) {
                                if (!isServer || conditional.matches(lootContext)) {
                                    modifiedDrag.setValue(conditional.effect().process(level, living.getRandom(), modifiedDrag.getValue()));
                                }
                            }
                        }

                        // Parse Homing.
                        List<ConditionalEffect<HomingProjectileEffect>> homingEffects = enchantment.value().effects().get(EnchantmentEffectComponentRegistry.PROJECTILE_HOMING);
                        if (homingEffects != null) {
                            for (ConditionalEffect<HomingProjectileEffect> conditional : homingEffects) {
                                if (!isServer || conditional.matches(lootContext)) {
                                    HomingProjectileEffect payload = conditional.effect();
                                    homingStrength.setValue(homingStrength.getValue() + payload.trackingStrength().calculate(level));
                                    prioritizeHead = prioritizeHead || payload.prioritizeHead();
                                    homingArmingDist = Math.max(homingArmingDist, payload.armingDistance().calculate(level));
                                    homingMinDist = Math.max(homingMinDist, payload.minDistance().calculate(level));
                                    homingMaxDist = Math.max(homingMaxDist, payload.maxDistance().calculate(level));
                                    homingFov = Math.max(homingFov, payload.fov().calculate(level));
                                    homingTurnRate = Math.max(homingTurnRate, payload.turnRate().calculate(level));
                                }
                            }
                        }

                        // Parse Ricochet.
                        List<ConditionalEffect<RicochetProjectileEffect>> ricochetEffects = enchantment.value().effects().get(EnchantmentEffectComponentRegistry.PROJECTILE_RICOCHET);
                        if (ricochetEffects != null) {
                            for (ConditionalEffect<RicochetProjectileEffect> conditional : ricochetEffects) {
                                if (!isServer || conditional.matches(lootContext)) {
                                    RicochetProjectileEffect payload = conditional.effect();
                                    ricochetBounces = Math.max(ricochetBounces, Math.round(payload.maxBounces().calculate(level)));
                                    ricochetRetention = Math.max(ricochetRetention, payload.velocityRetention().calculate(level));
                                }
                            }
                        }

                        // Parse Magnetism.
                        List<ConditionalEffect<MagneticProjectileEffect>> magneticEffects = enchantment.value().effects().get(EnchantmentEffectComponentRegistry.PROJECTILE_MAGNETIC);
                        if (magneticEffects != null) {
                            for (ConditionalEffect<MagneticProjectileEffect> conditional : magneticEffects) {
                                if (!isServer || conditional.matches(lootContext)) {
                                    MagneticProjectileEffect payload = conditional.effect();
                                    magneticStrength.setValue(magneticStrength.getValue() + payload.pullStrength().calculate(level));
                                    magneticPrioritizeHead = magneticPrioritizeHead || payload.prioritizeHead();
                                    magneticArmingDist = Math.max(magneticArmingDist, payload.armingDistance().calculate(level));
                                    magneticSearchRadius = Math.max(magneticSearchRadius, payload.searchRadius().calculate(level));
                                }
                            }
                        }

                        // Parse Shrapnel.
                        List<ConditionalEffect<ShrapnelProjectileEffect>> shrapnelEffects = enchantment.value().effects().get(EnchantmentEffectComponentRegistry.PROJECTILE_SHRAPNEL);
                        if (shrapnelEffects != null) {
                            for (ConditionalEffect<ShrapnelProjectileEffect> conditional : shrapnelEffects) {
                                if (!isServer || conditional.matches(lootContext)) {
                                    ShrapnelProjectileEffect payload = conditional.effect();
                                    state.enchantment_core$setShrapnelGenerations(Math.max(state.enchantment_core$getShrapnelGenerations(), Math.round(payload.generations().calculate(level))));
                                    state.enchantment_core$setShrapnelAmount(Math.max(state.enchantment_core$getShrapnelAmount(), Math.round(payload.amount().calculate(level))));
                                    state.enchantment_core$setShrapnelSpread(Math.max(state.enchantment_core$getShrapnelSpread(), payload.spreadDegrees().calculate(level)));
                                    state.enchantment_core$setShrapnelVelocityRetention(Math.max(state.enchantment_core$getShrapnelVelocityRetention(), payload.velocityRetention().calculate(level)));
                                    state.enchantment_core$setShrapnelDamageRetention(Math.max(state.enchantment_core$getShrapnelDamageRetention(), payload.damageRetention().calculate(level)));
                                    state.enchantment_core$setShrapnelTriggerBlock(state.enchantment_core$getShrapnelTriggerBlock() || payload.triggerOnBlock());
                                    state.enchantment_core$setShrapnelTriggerEntity(state.enchantment_core$getShrapnelTriggerEntity() || payload.triggerOnEntity());
                                }
                            }
                        }
                    }

                    // Push aggregated results to the State Accessor.
                    if (modifiedGravity.getValue() != 1.0f) {
                        state.enchantment_core$setGravityMultiplier((double) modifiedGravity.getValue());
                    }
                    if (modifiedDrag.getValue() != 1.0f) {
                        state.enchantment_core$setDragMultiplier((double) modifiedDrag.getValue());
                    }

                    if (homingStrength.getValue() > 0.0f) {
                        state.enchantment_core$setHomingStrength((double) homingStrength.getValue());
                        state.enchantment_core$setHomingPrioritizesHead(prioritizeHead);
                        state.enchantment_core$setHomingArmingDistance(homingArmingDist);
                        state.enchantment_core$setHomingMinDistance(homingMinDist);
                        state.enchantment_core$setHomingMaxDistance(homingMaxDist);
                        state.enchantment_core$setHomingFov(homingFov);
                        state.enchantment_core$setHomingTurnRate(homingTurnRate);
                    }

                    if (ricochetBounces > 0) {
                        state.enchantment_core$setBouncesRemaining(ricochetBounces);
                        state.enchantment_core$setRicochetRetention(ricochetRetention);
                    }

                    if (magneticStrength.getValue() > 0.0f) {
                        state.enchantment_core$setMagneticStrength((double) magneticStrength.getValue());
                        state.enchantment_core$setMagneticPrioritizesHead(magneticPrioritizeHead);
                        state.enchantment_core$setMagneticArmingDistance(magneticArmingDist);
                        state.enchantment_core$setMagneticSearchRadius(magneticSearchRadius);
                    }
                }
            }
        }
    }
}