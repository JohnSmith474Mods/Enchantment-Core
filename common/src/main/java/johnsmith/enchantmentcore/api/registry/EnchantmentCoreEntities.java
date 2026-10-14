package johnsmith.enchantmentcore.api.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import johnsmith.enchantmentcore.api.enchantment.spellfield.entity.SpellFieldAnchorEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Defines custom entity types utilized by the Enchantment Core library.
 * <p>
 * Defers {@link EntityType.Builder#build(String)} execution via suppliers so that
 * {@code BuiltInRegistries.ENTITY_TYPE.createIntrusiveHolder} is only invoked when the
 * entity registry is unfrozen during loader registration events.
 */
public class EnchantmentCoreEntities {

    private static final Map<String, Supplier<EntityType<?>>> ENTITIES = new LinkedHashMap<>();

    /**
     * The entity type definition for the spell field anchor.
     * Acts as an invisible, marker-based origin point for delayed or persistent spatial spell effects.
     * Populated when the entity type is built during registration.
     */
    public static EntityType<SpellFieldAnchorEntity> SPELL_FIELD_ANCHOR;

    static {
        create(
                "spell_field_anchor",
                () -> {
                    if (SPELL_FIELD_ANCHOR == null) {
                        SPELL_FIELD_ANCHOR = EntityType.Builder
                                .<SpellFieldAnchorEntity>of(SpellFieldAnchorEntity::new, MobCategory.MISC)
                                .sized(0.1F, 0.1F)
                                .clientTrackingRange(64)
                                .updateInterval(1)
                                .noSummon()
                                .build("spell_field_anchor");
                    }
                    return SPELL_FIELD_ANCHOR;
                }
        );
    }

    /**
     * Records an entity type supplier for deferred platform registration.
     *
     * @param name           The registry path identifier for the entity.
     * @param entitySupplier A supplier that builds and returns the configured entity type instance.
     * @param <T>            The bounded entity class.
     */
    private static <T extends Entity> void create(String name, Supplier<EntityType<T>> entitySupplier) {
        ENTITIES.put(name, entitySupplier::get);
    }

    /**
     * Retrieves an unmodifiable view of all defined entity type suppliers keyed by their path identifier.
     *
     * @return The map of entity type suppliers to register.
     */
    public static Map<String, Supplier<EntityType<?>>> getEntities() {
        return Collections.unmodifiableMap(ENTITIES);
    }

    /**
     * Triggers static initialization of entity type definitions.
     */
    public static void initialize() {}
}