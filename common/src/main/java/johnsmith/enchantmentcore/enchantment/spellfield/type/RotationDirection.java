package johnsmith.enchantmentcore.enchantment.spellfield.type;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum RotationDirection implements StringRepresentable {
    CLOCKWISE("clockwise"),
    COUNTER_CLOCKWISE("counter_clockwise"),
    ANY("any");

    public static final Codec<RotationDirection> CODEC = StringRepresentable.fromEnum(RotationDirection::values);
    private final String name;

    RotationDirection(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}