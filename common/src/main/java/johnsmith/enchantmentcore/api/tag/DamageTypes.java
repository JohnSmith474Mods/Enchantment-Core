package johnsmith.enchantmentcore.api.tag;

import johnsmith.enchantmentcore.Constants;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * Defines standard damage type tags for the enchantment core system.
 */
public final class DamageTypes {
    /**
     * Tag key identifying physical damage sources.
     */
    public static final TagKey<DamageType> IS_PHYSICAL_DAMAGE = create("is_physical_damage");

    /**
     * Tag key identifying magic damage sources.
     */
    public static final TagKey<DamageType> IS_MAGIC_DAMAGE = create("is_magic_damage");

    private static TagKey<net.minecraft.world.damagesource.DamageType> create(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name));
    }

    private DamageTypes() {}

    /**
     * Initializes the damage type tags.
     */
    public static void initialize() {}
}