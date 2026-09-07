package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.projectile;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;
import java.util.Optional;

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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin modifying the baseline acceleration vector of fired projectiles.
 * Corresponds to the {@code PROJECTILE_VELOCITY} enchantment component.
 */
@Mixin(Projectile.class)
public abstract class VelocityMixin {

    @Shadow @Nullable public abstract Entity getOwner();

    /**
     * Intercepts the {@code velocity} float variable at the head of the {@code shoot} method.
     * Integrates modifiers mapped on the shooter's active weapon to override flight speed.
     *
     * @param velocity The base velocity scalar determined by the engine logic.
     * @return The dynamically modified velocity scalar.
     */
    @ModifyVariable(method = "shoot(DDDFF)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float enchantment_core$modifyProjectileVelocity(float velocity) {
        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity living) {
            ItemStack weapon = living.getUseItem();
            if (weapon.isEmpty()) weapon = living.getMainHandItem();

            if (!weapon.isEmpty()) {
                ItemEnchantments enchantments = weapon.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

                if (!enchantments.isEmpty()) {
                    MutableFloat modifiedVelocity = new MutableFloat(velocity);
                    boolean isServer = living.level() instanceof ServerLevel;
                    LootContext lootContext = null;

                    if (isServer) {
                        LootParams params = new LootParams.Builder((ServerLevel) living.level())
                                .withParameter(LootContextParams.THIS_ENTITY, living)
                                .withParameter(LootContextParams.ENCHANTMENT_LEVEL, 1)
                                .withParameter(LootContextParams.ORIGIN, living.position())
                                .create(LootContextParamSets.ENCHANTED_ENTITY);
                        lootContext = new LootContext.Builder(params).create(Optional.empty());
                    }

                    for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                        Holder<Enchantment> enchantment = entry.getKey();
                        int level = entry.getIntValue();

                        List<ConditionalEffect<EnchantmentValueEffect>> velocityEffects = enchantment.value().effects().get(EnchantmentEffectComponentRegistry.PROJECTILE_VELOCITY);

                        if (velocityEffects != null) {
                            for (ConditionalEffect<EnchantmentValueEffect> conditional : velocityEffects) {
                                // Client actively predicts the effect modification to maintain accurate visual alignment.
                                if (!isServer || conditional.matches(lootContext)) {
                                    modifiedVelocity.setValue(conditional.effect().process(level, living.getRandom(), modifiedVelocity.getValue()));
                                }
                            }
                        }
                    }
                    return modifiedVelocity.getValue();
                }
            }
        }
        return velocity;
    }
}