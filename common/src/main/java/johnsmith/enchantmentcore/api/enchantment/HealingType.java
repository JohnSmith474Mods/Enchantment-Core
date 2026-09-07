package johnsmith.enchantmentcore.api.enchantment;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * Specifies the method of health restoration applied to an entity.
 */
public enum HealingType implements StringRepresentable {
    /**
     * Restores standard health points.
     */
    HEALING("healing"),

    /**
     * Adds temporary absorption hearts.
     */
    ABSORPTION("absorption");

    public static final Codec<HealingType> CODEC = StringRepresentable.fromEnum(HealingType::values);
    private final String name;

    HealingType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}