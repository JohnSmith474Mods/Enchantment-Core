package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.living.player;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.api.entity.accessor.StreakStateAccessor;
import johnsmith.enchantmentcore.enchantment.effect.BuoyancyEffect;
import johnsmith.enchantmentcore.enchantment.effect.StreakEffect;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting the Player class.
 * Intercepts block destruction speed calculations to apply dynamic enchantment multipliers.
 * Integrates the buoyancy and mining streak enchantment components.
 */
@Mixin(Player.class)
public abstract class DestroySpeedMixin {

    /**
     * Intercepts the return value of the block destruction speed calculation.
     * Evaluates active enchantments to negate fluid mining penalties or apply consecutive block mining bonuses.
     *
     * @param state The state of the block being mined.
     * @param cir   The callback information carrying the modifiable destruction speed scalar.
     */
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void enchantment_core$applyBreakSpeedModifiers(BlockState state, CallbackInfoReturnable<Float> cir) {
        float speed = cir.getReturnValueF();

        // Terminate evaluation if the base speed indicates no effective mining progress.
        if (speed <= 1.0F) return;

        Player player = (Player) (Object) this;
        boolean isClient = player.level().isClientSide();
        ServerLevel serverLevel = isClient ? null : (ServerLevel) player.level();

        float buoyancyMultiplier = 1.0F;
        float streakMultiplier = 1.0F;

        // Evaluate buoyancy compensation. Execute strictly when the player is submerged and not grounded.
        if (!player.onGround() && (player.isInWater() || player.isInLava())) {
            LootContext buoyancyContext = null;

            // Scan all equipment slots for active buoyancy enchantment effects.
            for (ItemStack equipment : player.getAllSlots()) {
                if (equipment.isEmpty()) continue;
                ItemEnchantments enchantments = equipment.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                if (enchantments.isEmpty()) continue;

                for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                    List<ConditionalEffect<BuoyancyEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.BUOYANCY.get());

                    if (effects != null) {
                        // Lazily instantiate the loot context exclusively on the server.
                        if (!isClient && buoyancyContext == null) {
                            LootParams params = new LootParams.Builder(serverLevel)
                                    .withParameter(LootContextParams.THIS_ENTITY, player)
                                    .withParameter(LootContextParams.ORIGIN, player.position())
                                    .withParameter(LootContextParams.ENCHANTMENT_LEVEL, entry.getIntValue())
                                    .create(LootContextParamSets.ENCHANTED_ENTITY);
                            buoyancyContext = new LootContext.Builder(params).create(Optional.empty());
                        }

                        for (ConditionalEffect<BuoyancyEffect> cond : effects) {
                            // The client bypasses condition matching to maintain visual mining synchronization.
                            if (isClient || cond.matches(buoyancyContext)) {
                                float mult = cond.effect().breakSpeedMultiplier().calculate(entry.getIntValue());
                                // Retain the highest available buoyancy multiplier.
                                if (mult > buoyancyMultiplier) buoyancyMultiplier = mult;
                            }
                        }
                    }
                }
            }
        }

        // Evaluate mining streak acceleration.
        // Identify the target block and retrieve the active streak counter from the player state.
        String targetId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        int currentStreak = ((StreakStateAccessor) player).enchantment_core$getStreakCount(targetId);

        if (currentStreak > 0) {
            // Restrict mining streak logic to the main hand item.
            ItemStack mainhand = player.getMainHandItem();
            if (!mainhand.isEmpty()) {
                ItemEnchantments enchantments = mainhand.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

                if (!enchantments.isEmpty()) {
                    LootContext streakContext = null;

                    for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                        List<ConditionalEffect<StreakEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.MINING_STREAK.get());

                        if (effects != null) {
                            if (!isClient && streakContext == null) {
                                LootParams params = new LootParams.Builder(serverLevel)
                                        .withParameter(LootContextParams.TOOL, mainhand)
                                        .withParameter(LootContextParams.ENCHANTMENT_LEVEL, entry.getIntValue())
                                        .create(LootContextParamSets.ENCHANTED_ITEM);
                                streakContext = new LootContext.Builder(params).create(Optional.empty());
                            }

                            for (ConditionalEffect<StreakEffect> cond : effects) {
                                if (isClient || cond.matches(streakContext)) {
                                    // Calculate the aggregate multiplier increment based on the active streak count.
                                    streakMultiplier += (cond.effect().increment().calculate(entry.getIntValue()) * currentStreak);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Apply calculated multipliers to the base destruction speed and overwrite the return value.
        if (buoyancyMultiplier > 1.0F || streakMultiplier > 1.0F) {
            cir.setReturnValue(speed * buoyancyMultiplier * streakMultiplier);
        }
    }
}