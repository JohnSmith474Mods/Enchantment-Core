package johnsmith.enchantmentcore.mixin.enchantment.effect.block;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;
import johnsmith.enchantmentcore.enchantment.effect.AutoSmeltEffect;
import johnsmith.enchantmentcore.enchantment.effect.BonusLootEffect;
import johnsmith.enchantmentcore.enchantment.effect.ExperienceYieldEffect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting the base Block class to inject enchantment effect evaluation during block destruction.
 * Handles drop modification (Auto-Smelt, Bonus Loot), experience multiplication, and custom post-mine entity effects.
 */
@Mixin(Block.class)
public abstract class EnchantmentEventsMixin {

    @Shadow protected abstract void popExperience(ServerLevel level, BlockPos pos, int amount);

    /**
     * Intercepts the generated list of item drops immediately before it is returned to the caller.
     * Evaluates and applies Auto-Smelt, Bonus Loot, and Experience Yield Multiplier effects.
     *
     * @param state       The block state being broken.
     * @param level       The server level.
     * @param pos         The block coordinate.
     * @param blockEntity The block entity data, if applicable.
     * @param entity      The entity breaking the block.
     * @param tool        The item stack used to break the block.
     * @param cir         The callback information containing the modifiable drop list.
     */
    @Inject(
        method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void enchantment_core$processDropModifiers(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity, Entity entity, ItemStack tool, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (tool == null || tool.isEmpty()) return;

        ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;

        // 1. Scan for relevant effect components to avoid building unnecessary loot contexts.
        boolean hasSmelt = false;
        boolean hasBonus = false;
        boolean hasExp = false;

        for (Holder<Enchantment> ench : enchantments.keySet()) {
            if (ench.value().effects().has(EnchantmentEffectComponentRegistry.AUTO_SMELT.get())) hasSmelt = true;
            if (ench.value().effects().has(EnchantmentEffectComponentRegistry.BONUS_LOOT.get())) hasBonus = true;
            if (ench.value().effects().has(EnchantmentEffectComponentRegistry.EXPERIENCE_YIELD_MULTIPLIER.get())) hasExp = true;
        }

        if (!hasSmelt && !hasBonus && !hasExp) return;

        // 2. Construct the primary block loot context required for effect conditional matching.
        LootParams.Builder paramsBuilder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool)
                .withParameter(LootContextParams.BLOCK_STATE, state);

        if (entity != null) paramsBuilder.withParameter(LootContextParams.THIS_ENTITY, entity);
        if (blockEntity != null) paramsBuilder.withParameter(LootContextParams.BLOCK_ENTITY, blockEntity);

        LootContext blockContext = new LootContext.Builder(paramsBuilder.create(LootContextParamSets.BLOCK)).create(Optional.empty());

        AutoSmeltEffect smeltEffect = null;
        int smeltLevel = 0;

        List<BonusLootEffect> activeBonusEffects = new ArrayList<>();
        List<Integer> bonusLevels = new ArrayList<>();

        float xpMultiplier = 1.0F;

        // 3. Resolve active effects against the built loot context.
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> ench = entry.getKey();
            int levelValue = entry.getIntValue();

            if (hasSmelt && smeltEffect == null) {
                List<ConditionalEffect<AutoSmeltEffect>> sEffects = ench.value().effects().get(EnchantmentEffectComponentRegistry.AUTO_SMELT.get());
                if (sEffects != null) {
                    for (ConditionalEffect<AutoSmeltEffect> cond : sEffects) {
                        if (cond.matches(blockContext)) {
                            smeltEffect = cond.effect();
                            smeltLevel = levelValue;
                            break;
                        }
                    }
                }
            }

            if (hasBonus) {
                List<ConditionalEffect<BonusLootEffect>> bEffects = ench.value().effects().get(EnchantmentEffectComponentRegistry.BONUS_LOOT.get());
                if (bEffects != null) {
                    for (ConditionalEffect<BonusLootEffect> cond : bEffects) {
                        if (cond.matches(blockContext)) {
                            activeBonusEffects.add(cond.effect());
                            bonusLevels.add(levelValue);
                        }
                    }
                }
            }

            if (hasExp) {
                List<ConditionalEffect<ExperienceYieldEffect>> eEffects = ench.value().effects().get(EnchantmentEffectComponentRegistry.EXPERIENCE_YIELD_MULTIPLIER.get());
                if (eEffects != null) {
                    LootParams expParams = new LootParams.Builder(level)
                            .withParameter(LootContextParams.TOOL, tool)
                            .withParameter(LootContextParams.ENCHANTMENT_LEVEL, levelValue)
                            .create(LootContextParamSets.ENCHANTED_ITEM);

                    LootContext expContext = new LootContext.Builder(expParams).create(Optional.empty());

                    for (ConditionalEffect<ExperienceYieldEffect> cond : eEffects) {
                        if (cond.matches(expContext)) {
                            xpMultiplier = Math.max(xpMultiplier, cond.effect().multiplier().calculate(levelValue));
                        }
                    }
                }
            }
        }

        if (smeltEffect == null && activeBonusEffects.isEmpty()) return;

        // 4. Initialize modifiable drop list and experience accumulator.
        List<ItemStack> drops = new ObjectArrayList<>(cir.getReturnValue() == null ? List.of() : cir.getReturnValue());
        float generatedXp = 0.0f;

        // 5. Execute Bonus Loot generation logic.
        if (!activeBonusEffects.isEmpty()) {
            int fortuneLevel = 0;
            Optional<Holder.Reference<Enchantment>> fortuneOpt = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.FORTUNE);
            if (fortuneOpt.isPresent()) {
                fortuneLevel = enchantments.getLevel(fortuneOpt.get());
            }

            for (int i = 0; i < activeBonusEffects.size(); i++) {
                BonusLootEffect effect = activeBonusEffects.get(i);
                int levelValue = bonusLevels.get(i);

                if (!effect.targetBlocks().contains(state.getBlockHolder())) continue;

                float chance = effect.chance().calculate(levelValue);
                if (level.random.nextFloat() >= chance) continue;

                Optional<Holder<Item>> randomReward = effect.rewardItems().getRandomElement(level.random);
                if (randomReward.isPresent()) {
                    int count = 1;
                    // Apply Fortune scaling to bonus item generation if present.
                    if (fortuneLevel > 0) {
                        count = Math.max(1, level.random.nextInt(fortuneLevel + 2));
                    }
                    drops.add(new ItemStack(randomReward.get().value(), count));
                }

                if (effect.rewardExperience()) {
                    generatedXp += (level.random.nextInt(3) + 1);
                }
            }
        }

        // 6. Execute Auto-Smelt transformation logic.
        if (smeltEffect != null && !drops.isEmpty()) {
            RecipeManager recipeManager = level.getServer().getRecipeManager();
            ServerPlayer serverPlayer = entity instanceof ServerPlayer sp ? sp : null;

            int totalAdditionalUsage = 0;
            int calculatedUsagePerDrop = (int) smeltEffect.additionalToolUsage().calculate(smeltLevel);

            List<ItemStack> smeltedDrops = new ObjectArrayList<>(drops.size());

            for (ItemStack drop : drops) {
                SingleRecipeInput input = new SingleRecipeInput(drop);
                Optional<RecipeHolder<SmeltingRecipe>> recipeOpt = recipeManager.getRecipeFor(RecipeType.SMELTING, input, level);

                if (recipeOpt.isPresent()) {
                    RecipeHolder<SmeltingRecipe> recipeHolder = recipeOpt.get();
                    ItemStack result = recipeHolder.value().assemble(input, level.registryAccess()).copy();
                    // Maintain original drop counts (e.g., 3 iron ore -> 3 iron ingots).
                    result.setCount(drop.getCount() * result.getCount());
                    smeltedDrops.add(result);

                    totalAdditionalUsage += calculatedUsagePerDrop * drop.getCount();
                    if (smeltEffect.dropXp()) {
                        generatedXp += recipeHolder.value().experience() * drop.getCount();
                    }
                } else {
                    smeltedDrops.add(drop);
                }
            }

            drops = smeltedDrops;

            // Apply durability penalty for smelted items.
            if (totalAdditionalUsage > 0) {
                tool.hurtAndBreak(totalAdditionalUsage, level, serverPlayer, item -> {});
            }
        }

        // 7. Process generated experience and handle fractional XP accumulation.
        if (generatedXp > 0.0f) {
            generatedXp *= xpMultiplier;

            // Retrieve previously stored fractional XP from the tool.
            float storedXp = tool.getOrDefault(EnchantmentEffectComponentRegistry.STORED_SMELTING_XP.get(), 0.0f) + generatedXp;

            if (storedXp >= 1.0f) {
                int xpToDrop = (int) storedXp;
                storedXp -= xpToDrop;
                ExperienceOrb.award(level, Vec3.atCenterOf(pos), xpToDrop);
            }

            // Persist remaining fractional XP.
            if (storedXp > 0.0f) {
                tool.set(EnchantmentEffectComponentRegistry.STORED_SMELTING_XP.get(), storedXp);
            } else {
                tool.remove(EnchantmentEffectComponentRegistry.STORED_SMELTING_XP.get());
            }
        }

        // 8. Replace original return value with the modified drop list.
        cir.setReturnValue(drops);
    }

    /**
     * Intercepts intrinsic block experience generation (e.g., Coal Ore, Diamond Ore).
     * Calculates the delta between the original sampled amount and the multiplied amount, and drops the difference.
     *
     * @param level          The server level.
     * @param pos            The block coordinate.
     * @param tool           The item stack used to break the block.
     * @param amountProvider The provider governing intrinsic XP drops.
     * @param ci             The callback information.
     */
    @Inject(method = "tryDropExperience", at = @At("TAIL"))
    private void enchantment_core$multiplyBlockExperience(ServerLevel level, BlockPos pos, ItemStack tool, IntProvider amountProvider, CallbackInfo ci) {
        if (tool == null || tool.isEmpty() || !level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) return;

        ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;

        float totalMultiplier = 1.0F;

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            List<ConditionalEffect<ExperienceYieldEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.EXPERIENCE_YIELD_MULTIPLIER.get());

            if (effects != null && !effects.isEmpty()) {
                LootParams params = new LootParams.Builder(level)
                        .withParameter(LootContextParams.TOOL, tool)
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

        if (totalMultiplier > 1.0F) {
            // Re-sample the provider to calculate the exact differential intended for this drop instance.
            int sampledAmount = amountProvider.sample(level.random);
            if (sampledAmount > 0) {
                int additional = Math.round(sampledAmount * totalMultiplier) - sampledAmount;
                if (additional > 0) this.popExperience(level, pos, additional);
            }
        }
    }

    /**
     * Injects execution of the POST_MINE enchantment component immediately following successful block destruction.
     * Functions identically to the vanilla POST_ATTACK component but operates within the block destruction pipeline.
     *
     * @param level       The executing level.
     * @param player      The player breaking the block.
     * @param pos         The coordinate of the broken block.
     * @param state       The state of the broken block.
     * @param blockEntity The block entity data, if applicable.
     * @param tool        The item stack used to break the block.
     * @param ci          The callback information.
     */
    @Inject(method = "playerDestroy", at = @At("HEAD"))
    private void enchantment_core$triggerPostMineEffects(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
        if (level.isClientSide() || tool == null || tool.isEmpty()) return;

        ItemEnchantments enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;

        LootContext lootContext = null;
        EnchantedItemInUse itemInUse = null;

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            List<ConditionalEffect<EnchantmentEntityEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.POST_MINE.get());

            if (effects != null && !effects.isEmpty()) {
                // Defer loot context instantiation until an active effect is confirmed.
                if (lootContext == null) {
                    LootParams.Builder paramsBuilder = new LootParams.Builder((ServerLevel) level)
                            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                            .withParameter(LootContextParams.TOOL, tool)
                            .withParameter(LootContextParams.THIS_ENTITY, player)
                            .withParameter(LootContextParams.BLOCK_STATE, state);

                    if (blockEntity != null) paramsBuilder.withParameter(LootContextParams.BLOCK_ENTITY, blockEntity);

                    lootContext = new LootContext.Builder(paramsBuilder.create(LootContextParamSets.BLOCK)).create(Optional.empty());
                    itemInUse = new EnchantedItemInUse(tool, EquipmentSlot.MAINHAND, player);
                }

                for (ConditionalEffect<EnchantmentEntityEffect> conditionalEffect : effects) {
                    if (conditionalEffect.matches(lootContext)) {
                        conditionalEffect.effect().apply((ServerLevel) level, entry.getIntValue(), itemInUse, player, Vec3.atCenterOf(pos));
                    }
                }
            }
        }
    }
}