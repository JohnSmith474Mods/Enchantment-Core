package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.block.Block;

/**
 * Data-driven enchantment effect component that grants a probability to roll bonus item drops and
 * experience orbs when breaking blocks matching a specified {@link HolderSet}.
 * <p>
 * Evaluated during block drop generation in {@code EnchantmentEventsMixin}, where bonus item quantities
 * also scale with the tool's Fortune level if present.
 *
 * @param chance           The {@link LevelBasedValue} governing the probability (0.0 to 1.0) of triggering bonus loot.
 * @param targetBlocks     The {@link HolderSet} of blocks eligible to trigger this bonus loot effect when broken.
 * @param rewardItems      The pool of candidate {@link Item}s from which a random bonus drop is selected.
 * @param rewardExperience Whether triggering bonus loot also awards a small amount of experience.
 */
public record BonusLootEffect(
        LevelBasedValue chance,
        HolderSet<Block> targetBlocks,
        HolderSet<Item> rewardItems,
        boolean rewardExperience
) {
    /** JSON field key for the drop trigger probability. */
    public static final String CHANCE = "chance";

    /** JSON field key for the eligible target blocks set. */
    public static final String TARGET_BLOCKS = "target_blocks";

    /** JSON field key for the reward item pool. */
    public static final String REWARD_ITEMS = "reward_items";

    /** JSON field key for toggling bonus experience drops. */
    public static final String REWARD_EXPERIENCE = "reward_experience";

    /** Registry path identifier for this effect component. */
    public static final String KEY = "bonus_loot";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(CHANCE);

    /**
     * Codec for serializing and deserializing {@link BonusLootEffect} instances from JSON.
     */
    public static final Codec<BonusLootEffect> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    LevelBasedValue.CODEC.fieldOf(CHANCE).forGetter(BonusLootEffect::chance),
                    RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf(TARGET_BLOCKS).forGetter(BonusLootEffect::targetBlocks),
                    RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf(REWARD_ITEMS).forGetter(BonusLootEffect::rewardItems),
                    Codec.BOOL.optionalFieldOf(REWARD_EXPERIENCE, false).forGetter(BonusLootEffect::rewardExperience)
            ).apply(instance, BonusLootEffect::new)
    );
}