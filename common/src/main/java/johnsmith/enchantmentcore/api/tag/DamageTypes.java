package johnsmith.enchantmentcore.api.tag;

import johnsmith.enchantmentcore.Constants;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import org.jetbrains.annotations.NotNull;

/**
 * Defines standard {@link DamageType} tag keys provided by Enchantment Core for categorizing
 * physical and magical damage sources in enchantment predicates.
 */
public final class DamageTypes {
    /**
     * Tag key ({@code #enchantment_core:is_physical_damage}) identifying physical kinetic damage sources
     * (such as melee attacks, explosions, falling blocks, and kinetic wall impacts).
     */
    public static final TagKey<@NotNull DamageType> IS_PHYSICAL_DAMAGE = create("is_physical_damage");

    /**
     * Tag key ({@code #enchantment_core:is_magic_damage}) identifying magical damage sources
     * (such as direct magic, indirect magic, thorns, and sonic boom).
     */
    public static final TagKey<@NotNull DamageType> IS_MAGIC_DAMAGE = create("is_magic_damage");

    private static TagKey<@NotNull DamageType> create(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name));
    }

    private DamageTypes() {}

    /**
     * Triggers static initialization of the damage type tags.
     */
    public static void initialize() {}
}