package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.living;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.enchantment.effect.DamageHealingEffect;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin isolating the logic for healing or generating absorption based on damage dealt.
 */
@Mixin(LivingEntity.class)
public abstract class OnDamageHealingMixin {

    /**
     * Triggers health restoration or absorption generation when the attacker deals damage.
     *
     * @param damageSource The damage source context.
     * @param damageAmount The total damage inflicted.
     * @param ci           Callback control handle.
     */
    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void enchantment_core$applyDamageHealing(DamageSource damageSource, float damageAmount, CallbackInfo ci) {
        if (damageAmount <= 0.0F) return;

        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide()) return;

        Entity attacker = damageSource.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        ItemStack weapon = livingAttacker.getMainHandItem();
        if (weapon.isEmpty()) return;

        ItemEnchantments enchantments = weapon.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;

        LootContext lootContext = null;

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            List<ConditionalEffect<DamageHealingEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.ON_DAMAGE_HEALING);

            if (effects != null && !effects.isEmpty()) {
                if (lootContext == null) {
                    LootParams params = new LootParams.Builder((ServerLevel) target.level())
                            .withParameter(LootContextParams.THIS_ENTITY, target)
                            .withParameter(LootContextParams.ORIGIN, livingAttacker.position())
                            .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                            .withParameter(LootContextParams.ATTACKING_ENTITY, attacker)
                            .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, damageSource.getDirectEntity())
                            .withParameter(LootContextParams.ENCHANTMENT_LEVEL, entry.getIntValue())
                            .create(LootContextParamSets.ENCHANTED_DAMAGE);
                    lootContext = new LootContext.Builder(params).create(Optional.empty());
                }

                for (ConditionalEffect<DamageHealingEffect> cond : effects) {
                    if (cond.matches(lootContext)) {
                        // Forward the raw inflicted damage amount to calculate healed volume.
                        cond.effect().apply(livingAttacker, entry.getIntValue(), damageAmount);
                    }
                }
            }
        }
    }
}