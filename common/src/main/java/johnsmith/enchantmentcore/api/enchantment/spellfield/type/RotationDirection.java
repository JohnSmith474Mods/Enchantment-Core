package johnsmith.enchantmentcore.api.enchantment.spellfield.type;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

import org.jetbrains.annotations.NotNull;

/**
 * Specifies the tangential orbital chirality (direction of rotation) for entities affected by
 * {@link johnsmith.enchantmentcore.api.enchantment.spellfield.effect.entity.OrbitEffect}.
 */
public enum RotationDirection implements StringRepresentable {
    /** Forces all affected entities to orbit in a clockwise direction ($-1$ tangential sign). */
    CLOCKWISE("clockwise"),

    /** Forces all affected entities to orbit in a counter-clockwise direction ($+1$ tangential sign). */
    COUNTER_CLOCKWISE("counter_clockwise"),

    /** Assigns orbital direction deterministically per entity based on the parity of {@link net.minecraft.world.entity.Entity#getId()}. */
    ANY("any");

    /**
     * Codec for serializing and deserializing {@link RotationDirection} constants by their lowercase identifier.
     */
    public static final Codec<RotationDirection> CODEC = StringRepresentable.fromEnum(RotationDirection::values);

    /** The serialized string identifier of this rotation mode. */
    private final String name;

    RotationDirection(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
}