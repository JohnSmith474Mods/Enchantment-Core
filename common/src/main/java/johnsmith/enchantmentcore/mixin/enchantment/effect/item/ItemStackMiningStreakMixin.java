package johnsmith.enchantmentcore.mixin.enchantment.effect.item;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.api.entity.accessor.StreakStateAccessor;
import johnsmith.enchantmentcore.enchantment.effect.StreakEffect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting the ItemStack class to evaluate and apply mining streak enchantment effects.
 * Intercepts the block mining event to increment a streak counter on the player when blocks
 * are destroyed consecutively.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMiningStreakMixin {

    /**
     * Injects logic at the beginning of the block mining process.
     * Evaluates the item's enchantments for the MINING_STREAK component and updates the player's
     * streak state if the conditional requirements are met.
     *
     * @param level  The level where the block is being mined.
     * @param state  The block state being broken.
     * @param pos    The coordinate of the block.
     * @param player The player performing the mining action.
     * @param ci     The callback information.
     */
    @Inject(method = "mineBlock", at = @At("HEAD"))
    private void enchantment_core$incrementMiningStreak(Level level, BlockState state, BlockPos pos, Player player, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;

        boolean isClient = level.isClientSide();
        LootContext lootContext = null;

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            // Restrict streak accumulation to items held in the main hand.
            if (!entry.getKey().value().matchingSlot(EquipmentSlot.MAINHAND)) continue;

            List<ConditionalEffect<StreakEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.MINING_STREAK.get());
            if (effects != null) {
                // Lazily instantiate the loot context only on the server and only if an effect exists.
                if (!isClient && lootContext == null) {
                    LootParams params = new LootParams.Builder((ServerLevel) level)
                            .withParameter(LootContextParams.TOOL, stack)
                            .withParameter(LootContextParams.ENCHANTMENT_LEVEL, entry.getIntValue())
                            .create(LootContextParamSets.ENCHANTED_ITEM);

                    lootContext = new LootContext.Builder(params).create(Optional.empty());
                }

                for (ConditionalEffect<StreakEffect> cond : effects) {
                    // Client blindly predicts the streak increment to ensure visual/UI responsiveness.
                    // Server actually validates the condition via the loot context.
                    if (isClient || cond.matches(lootContext)) {
                        // Use the block's registry key as the unique streak identifier.
                        String targetId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                        int maxStreak = (int) cond.effect().maxStreak().calculate(entry.getIntValue());

                        // Push the increment to the player's streak accessor.
                        ((StreakStateAccessor) player).enchantment_core$incrementStreak(targetId, maxStreak, cond.effect().timeoutTicks());
                    }
                }
            }
        }
    }
}