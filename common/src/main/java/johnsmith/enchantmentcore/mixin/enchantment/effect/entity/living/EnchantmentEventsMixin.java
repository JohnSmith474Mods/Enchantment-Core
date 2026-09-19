package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.living;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin injecting enchantment lifecycle dispatchers into {@link LivingEntity}.
 * Fires custom hooks: fatal damage survival, jumping, shield parrying, and item consumption cycles.
 */
@Mixin(LivingEntity.class)
public abstract class EnchantmentEventsMixin {

    /**
     * Evaluates a targeted effect component collection on a specific item stack.
     *
     * @param item          The source item stack.
     * @param componentType The registry component to match.
     * @param damageSource  Optional damage source if context is combat-driven.
     */
    @Unique
    private void enchantment_core$evaluateEffect(ItemStack item, DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> componentType, DamageSource damageSource) {
        if (item == null || item.isEmpty()) return;
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) return;

        ItemEnchantments enchantments = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;

        LootParams.Builder paramsBuilder = null;
        EnchantedItemInUse inUse = null;

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            List<ConditionalEffect<EnchantmentEntityEffect>> effects = entry.getKey().value().effects().get(componentType);

            if (effects != null && !effects.isEmpty()) {
                if (paramsBuilder == null) {
                    paramsBuilder = new LootParams.Builder((ServerLevel) entity.level())
                            .withParameter(LootContextParams.THIS_ENTITY, entity)
                            .withParameter(LootContextParams.ORIGIN, entity.position());

                    if (damageSource != null) {
                        paramsBuilder.withParameter(LootContextParams.DAMAGE_SOURCE, damageSource);
                        if (damageSource.getEntity() != null) {
                            paramsBuilder.withParameter(LootContextParams.ATTACKING_ENTITY, damageSource.getEntity());
                        }
                    }
                    inUse = new EnchantedItemInUse(item, EquipmentSlot.MAINHAND, entity);
                }

                LootParams params = paramsBuilder.withParameter(LootContextParams.ENCHANTMENT_LEVEL, entry.getIntValue()).create(LootContextParamSets.ENCHANTED_ENTITY);
                LootContext lootContext = new LootContext.Builder(params).create(Optional.empty());

                for (ConditionalEffect<EnchantmentEntityEffect> conditionalEffect : effects) {
                    if (conditionalEffect.matches(lootContext)) {
                        conditionalEffect.effect().apply((ServerLevel) entity.level(), entry.getIntValue(), inUse, entity, entity.position());
                    }
                }
            }
        }
    }

    /**
     * Evaluates a targeted effect component across all worn armor and held items.
     */
    @Unique
    private void enchantment_core$evaluateAllSlots(DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> componentType, DamageSource damageSource) {
        LivingEntity entity = (LivingEntity) (Object) this;
        for (ItemStack equipment : entity.getAllSlots()) {
            enchantment_core$evaluateEffect(equipment, componentType, damageSource);
        }
    }

    /**
     * Injects before totem death saves to process custom fatal damage mechanics.
     */
    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"))
    private void enchantment_core$onFatalDamage(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        enchantment_core$evaluateAllSlots(EnchantmentEffectComponentRegistry.FATAL_DAMAGE.get(), damageSource);
    }

    /**
     * Injects at the end of vertical jump impulses to trigger post-jump events.
     */
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void enchantment_core$onJump(CallbackInfo ci) {
        enchantment_core$evaluateAllSlots(EnchantmentEffectComponentRegistry.ON_JUMP.get(), null);
    }

    /**
     * Injects when an active shield absorbs attack damage.
     */
    @Inject(method = "hurtCurrentlyUsedShield", at = @At("HEAD"))
    private void enchantment_core$onShieldBlock(float damage, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        enchantment_core$evaluateEffect(entity.getUseItem(), EnchantmentEffectComponentRegistry.SHIELD_BLOCK.get(), null);
    }

    /**
     * Injects when an entity initiates the channel or charge of an item.
     */
    @Inject(method = "startUsingItem", at = @At("TAIL"))
    private void enchantment_core$onItemUseStart(InteractionHand hand, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack item = entity.getItemInHand(hand);
        enchantment_core$evaluateEffect(item, EnchantmentEffectComponentRegistry.ITEM_USE_START.get(), null);
    }

    /**
     * Injects every active tick that an item channel continues.
     */
    @Inject(method = "updateUsingItem", at = @At("HEAD"))
    private void enchantment_core$onItemUseTick(ItemStack usingItem, CallbackInfo ci) {
        enchantment_core$evaluateEffect(usingItem, EnchantmentEffectComponentRegistry.ITEM_USE_TICK.get(), null);
    }
}