package johnsmith.enchantmentcore.api.config;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

import org.jetbrains.annotations.NotNull;

/**
 * Defines the activation and inclusion behavior for built-in mod data packs and resource packs.
 */
public enum PackInclusionType implements StringRepresentable {
    /**
     * The built-in pack is registered as optional and disabled by default, allowing users to manually
     * enable it via the pack selection screen or server configuration.
     */
    OPTIONAL("optional"),

    /**
     * The built-in pack is enabled by default upon world creation or client startup, but can still be
     * manually disabled by the user.
     */
    ACTIVE("active"),

    /**
     * The built-in pack is forcefully enabled at all times and cannot be disabled by the user.
     */
    REQUIRED("required");

    /**
     * Codec for serializing and deserializing {@link PackInclusionType} constants by their lowercase identifier.
     */
    public static final Codec<PackInclusionType> CODEC = StringRepresentable.fromEnum(PackInclusionType::values);

    /** The serialized string identifier of this inclusion mode. */
    private final String id;

    /**
     * Constructs a {@link PackInclusionType} enum constant.
     *
     * @param id The lowercase string identifier used for serialization.
     */
    PackInclusionType(String id) {
        this.id = id;
    }

    /**
     * Returns the serialized string representation of this pack inclusion mode.
     *
     * @return The lowercase identifier string.
     */
    @Override
    public @NotNull String getSerializedName() {
        return this.id;
    }
}