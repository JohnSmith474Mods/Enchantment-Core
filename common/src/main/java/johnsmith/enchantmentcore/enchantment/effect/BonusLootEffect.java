package johnsmith.enchantmentcore.enchantment.effect;

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

public record BonusLootEffect(
        LevelBasedValue chance,
        HolderSet<Block> targetBlocks,
        HolderSet<Item> rewardItems,
        boolean rewardExperience
) {
    public static final String CHANCE = "chance";
    public static final String TARGET_BLOCKS = "target_blocks";
    public static final String REWARD_ITEMS = "reward_items";
    public static final String REWARD_EXPERIENCE = "reward_experience";

    public static final String KEY = "bonus_loot";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(CHANCE);

    public static final Codec<BonusLootEffect> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    LevelBasedValue.CODEC.fieldOf(CHANCE).forGetter(BonusLootEffect::chance),
                    RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf(TARGET_BLOCKS).forGetter(BonusLootEffect::targetBlocks),
                    RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf(REWARD_ITEMS).forGetter(BonusLootEffect::rewardItems),
                    Codec.BOOL.optionalFieldOf(REWARD_EXPERIENCE, false).forGetter(BonusLootEffect::rewardExperience)
            ).apply(instance, BonusLootEffect::new)
    );
}