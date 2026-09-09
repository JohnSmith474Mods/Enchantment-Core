package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.living;

import com.llamalad7.mixinextras.sugar.Local;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.api.entity.accessor.StreakStateAccessor;
import johnsmith.enchantmentcore.enchantment.effect.StreakEffect;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin isolating the combat streak scaling mechanics applied during incoming damage calculation.
 */
@Mixin(LivingEntity.class)
public abstract class CombatStreakMixin {

    /**
     * Intercepts incoming damage calculation to apply consecutive hit damage streaks.
     *
     * @param damageAmount The base incoming damage value.
     * @param damageSource The damage source context.
     * @return The modified damage value scaled by the active streak.
     */
    @ModifyVariable(
            method = "actuallyHurt",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float enchantment_core$applyDamageStreak(float damageAmount, @Local ServerLevel serverLevel, @Local DamageSource damageSource) {
        if (damageAmount <= 0.0F) return damageAmount; // Ignore zero or negative damage events.

        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide()) return damageAmount; // Process combat logic on the server only.

        Entity attacker = damageSource.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return damageAmount;

        ItemStack weapon = livingAttacker.getMainHandItem();
        if (weapon.isEmpty()) return damageAmount;

        ItemEnchantments enchantments = weapon.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return damageAmount;

        // Identify the target type to group streak tracking per entity species.
        String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        int currentStreak = ((StreakStateAccessor) livingAttacker).enchantment_core$getStreakCount(targetId);
        float multiplier = 1.0F;

        LootContext lootContext = null;

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            List<ConditionalEffect<StreakEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.DAMAGE_STREAK.get());

            if (effects != null && !effects.isEmpty()) {
                // Lazily instantiate the loot context for condition validation.
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

                for (ConditionalEffect<StreakEffect> cond : effects) {
                    if (cond.matches(lootContext)) {
                        // Increase the damage multiplier by the configured per-hit bonus.
                        multiplier += (cond.effect().increment().calculate(entry.getIntValue()) * currentStreak);

                        int maxStreak = (int) cond.effect().maxStreak().calculate(entry.getIntValue());
                        ((StreakStateAccessor) livingAttacker).enchantment_core$incrementStreak(targetId, maxStreak, cond.effect().timeoutTicks());
                    }
                }
            }
        }

        return damageAmount * multiplier;
    }
}