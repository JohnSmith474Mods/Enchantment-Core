package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.living;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.enchantment.effect.ExperienceYieldEffect;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting entity experience drops upon death in {@link LivingEntity#getExperienceReward}.
 * Aggregates and applies multipliers from the killer's equipped gear.
 */
@Mixin(LivingEntity.class)
public abstract class ExperienceMixin {

    /**
     * Multiplies the base experience reward dropped by the entity upon death.
     *
     * @param level    The server level.
     * @param attacker The killing entity.
     * @param cir      Returnable integer callback storing the final dropped experience.
     */
    @Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
    private void enchantment_core$multiplyEntityExperience(ServerLevel level, Entity attacker, CallbackInfoReturnable<Integer> cir) {
        int originalXp = cir.getReturnValueI();
        if (originalXp <= 0 || !(attacker instanceof LivingEntity livingAttacker)) return;

        float totalMultiplier = 1.0F;

        // Iterate all equipment slots on the attacking entity to compound experience modifiers.
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack weapon = livingAttacker.getItemBySlot(slot);
            if (weapon.isEmpty()) continue;

            ItemEnchantments enchantments = weapon.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (enchantments.isEmpty()) continue;

            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                if (!entry.getKey().value().matchingSlot(slot)) continue;

                List<ConditionalEffect<ExperienceYieldEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.EXPERIENCE_YIELD_MULTIPLIER.get());

                if (effects != null && !effects.isEmpty()) {
                    LootParams params = new LootParams.Builder(level)
                            .withParameter(LootContextParams.TOOL, weapon)
                            .withParameter(LootContextParams.ENCHANTMENT_LEVEL, entry.getIntValue())
                            .create(LootContextParamSets.ENCHANTED_ITEM);

                    LootContext lootContext = new LootContext.Builder(params).create(Optional.empty());

                    for (ConditionalEffect<ExperienceYieldEffect> conditionalEffect : effects) {
                        if (conditionalEffect.matches(lootContext)) {
                            totalMultiplier *= conditionalEffect.effect().multiplier().calculate(entry.getIntValue());
                        }
                    }
                }
            }
        }

        if (totalMultiplier != 1.0F) {
            cir.setReturnValue(Math.round(originalXp * totalMultiplier));
        }
    }
}