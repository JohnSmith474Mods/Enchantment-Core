package johnsmith.enchantmentcore.api.config.data;

import com.mojang.serialization.Codec;

import java.util.Objects;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Data container holding either a concrete item or an item tag key.
 */
public class ItemOrItems {
    private final Item item;
    private final TagKey<Item> tag;

    /**
     * Initializes the container with a specific item.
     *
     * @param item The target item.
     */
    public ItemOrItems(Item item) {
        this.item = item;
        this.tag = null;
    }

    /**
     * Initializes the container with an item tag key.
     *
     * @param tag The target item tag key.
     */
    public ItemOrItems(TagKey<Item> tag) {
        this.item = null;
        this.tag = tag;
    }

    /**
     * Identifies if the container holds a tag key.
     *
     * @return True if the container holds a tag key, false otherwise.
     */
    public boolean isTag() {
        return this.tag != null;
    }

    /**
     * Retrieves the stored item.
     *
     * @return The stored item, or null if the container holds a tag key.
     */
    public Item getItem() {
        return this.item;
    }

    /**
     * Retrieves the stored tag key.
     *
     * @return The stored tag key, or null if the container holds an item.
     */
    public TagKey<Item> getTag() {
        return this.tag;
    }

    /**
     * Converts the stored data into its string representation.
     *
     * @return The string representation of the item or tag key.
     */
    public String asString() {
        if (this.isTag()) {
            return "#" + this.tag.location();
        } else {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(this.item);
            return key != null ? key.toString() : "minecraft:air";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ItemOrItems that = (ItemOrItems) obj;
        return Objects.equals(this.item, that.item) && Objects.equals(this.tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.item, this.tag);
    }

    /**
     * Codec for serialization and deserialization of the container data.
     */
    public static final Codec<ItemOrItems> CODEC = Codec.STRING.xmap(
            str -> {
                if (str.startsWith("#")) {
                    return new ItemOrItems(TagKey.create(Registries.ITEM, ResourceLocation.parse(str.substring(1))));
                } else {
                    return new ItemOrItems(BuiltInRegistries.ITEM.get(ResourceLocation.parse(str)).get().value());
                }
            },
            ItemOrItems::asString
    );
}