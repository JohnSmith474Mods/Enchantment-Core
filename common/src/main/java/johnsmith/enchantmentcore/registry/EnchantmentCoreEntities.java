package johnsmith.enchantmentcore.registry;

import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.enchantment.spellfield.entity.SpellFieldAnchorEntity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Manages the registration of custom entity types utilized by the Enchantment Core library.
 */
public class EnchantmentCoreEntities {

    /**
     * The entity type definition for the spell field anchor.
     * Acts as an invisible, marker-based origin point for delayed or persistent spatial spell effects.
     */
    public static final EntityType<SpellFieldAnchorEntity> SPELL_FIELD_ANCHOR = register(
            "spell_field_anchor",
            EntityType.Builder
                    .<SpellFieldAnchorEntity>of(SpellFieldAnchorEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .noSummon()
    );

    /**
     * Helper method to register an entity type into the built-in Minecraft registry.
     *
     * @param name    The registry path identifier for the entity.
     * @param builder The configured entity type builder.
     * @param <T>     The bounded entity class.
     * @return The registered entity type.
     */
    public static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name);
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    /**
     * Initializes the entity registry. Must be invoked during the mod's common setup phase.
     */
    public static void initialize() {}
}