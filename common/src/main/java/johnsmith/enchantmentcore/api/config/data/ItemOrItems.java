package johnsmith.enchantmentcore.api.config.data;

import com.mojang.serialization.Codec;

import java.util.Objects;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import org.jetbrains.annotations.Nullable;

/**
 * Immutable sum-type data container representing either a single registered {@link Item} or an
 * item {@link TagKey} (prefixed with {@code "#"} when serialized).
 * <p>
 * Used by Enchantment Core's configurable item set properties and GUI selection screens to allow
 * users and datapacks to mix individual items and item tags seamlessly within a single list.
 */
public class ItemOrItems {
    /** The concrete item instance, or {@code null} if this container represents an item tag. */
    private final @Nullable Item item;

    /** The item tag key, or {@code null} if this container represents a single item. */
    private final @Nullable TagKey<Item> tag;

    /**
     * Constructs an {@link ItemOrItems} container holding a single concrete {@link Item}.
     *
     * @param item The target item instance.
     */
    public ItemOrItems(Item item) {
        this.item = item;
        this.tag = null;
    }

    /**
     * Constructs an {@link ItemOrItems} container holding an item {@link TagKey}.
     *
     * @param tag The target item tag key.
     */
    public ItemOrItems(TagKey<Item> tag) {
        this.item = null;
        this.tag = tag;
    }

    /**
     * Checks whether this container holds an item {@link TagKey} rather than a single {@link Item}.
     *
     * @return {@code true} if this instance represents a tag key, {@code false} if it represents a single item.
     */
    public boolean isTag() {
        return this.tag != null;
    }

    /**
     * Retrieves the stored {@link Item} instance.
     *
     * @return The stored {@link Item}, or {@code null} if {@link #isTag()} is {@code true}.
     */
    public @Nullable Item getItem() {
        return this.item;
    }

    /**
     * Retrieves the stored item {@link TagKey}.
     *
     * @return The stored {@link TagKey}, or {@code null} if {@link #isTag()} is {@code false}.
     */
    public @Nullable TagKey<Item> getTag() {
        return this.tag;
    }

    /**
     * Converts the stored item or tag key into its canonical string representation.
     * <p>
     * Tag keys are formatted with a leading {@code "#"} (e.g., {@code "#minecraft:enchantable/sword"}),
     * while single items are formatted as their registry resource location (e.g., {@code "minecraft:trident"}).
     *
     * @return The string representation of this entry, defaulting to {@code "minecraft:air"} if the item is unregistered.
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
     * Codec for serializing and deserializing {@link ItemOrItems} instances to and from strings.
     * Strings prefixed with {@code "#"} are parsed as item {@link TagKey}s; all other strings are
     * resolved against {@link BuiltInRegistries#ITEM}.
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