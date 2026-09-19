package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.living;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.enchantment.effect.TransparencyEffect;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
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
 * Mixin modifying armor detection exposure in {@link LivingEntity#getArmorCoverPercentage}.
 * Reduces mob line-of-sight detection range for translucent or mitigated armor pieces.
 */
@Mixin(LivingEntity.class)
public abstract class VisibilityMixin {

    /**
     * Intercepts calculation of armor visibility coverage.
     * Scales down detection penalties based on configured transparency mitigation.
     *
     * @param cir Returnable float callback with the final armor coverage ratio.
     */
    @Inject(method = "getArmorCoverPercentage", at = @At("RETURN"), cancellable = true)
    private void enchantment_core$mitigateArmorVisibility(CallbackInfoReturnable<Float> cir) {
        float originalCover = cir.getReturnValueF();
        if (originalCover <= 0.0F) return;

        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) return;

        ServerLevel serverLevel = (ServerLevel) entity.level();
        int totalSlots = 0;
        float unmitigatedPieces = 0.0F;

        // Iterate armor inventory slots to evaluate individual mitigation values.
        for (ItemStack stack : entity.getArmorSlots()) {
            totalSlots++;
            if (stack.isEmpty()) continue;

            float pieceMitigation = 0.0F;
            ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

            if (!enchantments.isEmpty()) {
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                    List<ConditionalEffect<TransparencyEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.TRANSPARENCY.get());

                    if (effects != null) {
                        LootParams params = new LootParams.Builder(serverLevel)
                                .withParameter(LootContextParams.TOOL, stack)
                                .withParameter(LootContextParams.ENCHANTMENT_LEVEL, entry.getIntValue())
                                .create(LootContextParamSets.ENCHANTED_ITEM);

                        LootContext lootContext = new LootContext.Builder(params).create(Optional.empty());

                        for (ConditionalEffect<TransparencyEffect> cond : effects) {
                            if (cond.matches(lootContext) && cond.effect().detectionMitigation().isPresent()) {
                                pieceMitigation = Math.max(pieceMitigation, cond.effect().detectionMitigation().get().calculate(entry.getIntValue()));
                            }
                        }
                    }
                }
            }

            // A fully mitigated piece contributes 0.0 towards the visibility detection profile.
            unmitigatedPieces += Math.max(0.0F, 1.0F - pieceMitigation);
        }

        if (totalSlots > 0) {
            float newCover = unmitigatedPieces / (float) totalSlots;
            // Guarantee the new coverage ratio cannot exceed original vanilla coverage.
            cir.setReturnValue(Math.max(0.0F, Math.min(newCover, originalCover)));
        }
    }
}