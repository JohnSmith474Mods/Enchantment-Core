package johnsmith.enchantmentcore.api.config;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum PackInclusionType implements StringRepresentable {
    OPTIONAL("optional"),
    ACTIVE("active"),
    REQUIRED("required");

    public static final Codec<PackInclusionType> CODEC = StringRepresentable.fromEnum(PackInclusionType::values);

    private final String id;

    PackInclusionType(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}