package johnsmith.enchantmentcore.api.tag;

import johnsmith.enchantmentcore.Constants;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Defines standard item tags for the enchantment core system.
 */
public final class ItemTags {
    /**
     * Tag key identifying all enchantable melee weapons.
     */
    public static final TagKey<Item> ENCHANTABLE_ALL_MELEE = create("enchantable/all_melee");

    /**
     * Tag key identifying all enchantable ranged weapons.
     */
    public static final TagKey<Item> ENCHANTABLE_ALL_RANGED = create("enchantable/all_ranged");

    private static TagKey<net.minecraft.world.item.Item> create(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name));
    }

    private ItemTags() {}

    /**
     * Initializes the item tags.
     */
    public static void initialize() {}
}