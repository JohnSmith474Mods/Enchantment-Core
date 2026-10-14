package johnsmith.enchantmentcore.api.enchantment;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

import org.jetbrains.annotations.NotNull;

/**
 * Specifies the health restoration mechanism applied to a target entity by healing-based
 * enchantment effects such as {@link johnsmith.enchantmentcore.api.enchantment.effect.HealingEntityEffect}
 * and {@link johnsmith.enchantmentcore.api.enchantment.effect.DamageHealingEffect}.
 */
public enum HealingType implements StringRepresentable {
    /**
     * Restores standard health points directly via {@link net.minecraft.world.entity.LivingEntity#heal(float)}.
     */
    HEALING("healing"),

    /**
     * Grants temporary absorption hearts via {@link net.minecraft.world.entity.LivingEntity#setAbsorptionAmount(float)},
     * expanding the entity's {@link net.minecraft.world.entity.ai.attributes.Attributes#MAX_ABSORPTION} base value if necessary.
     */
    ABSORPTION("absorption");

    /**
     * Codec for serializing and deserializing {@link HealingType} constants by their lowercase identifier.
     */
    public static final Codec<HealingType> CODEC = StringRepresentable.fromEnum(HealingType::values);

    /** The serialized string identifier of this healing mode. */
    private final String name;

    /**
     * Constructs a {@link HealingType} enum constant.
     *
     * @param name The lowercase string identifier used for serialization.
     */
    HealingType(String name) {
        this.name = name;
    }

    /**
     * Returns the serialized string representation of this healing type.
     *
     * @return The lowercase identifier string.
     */
    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
}