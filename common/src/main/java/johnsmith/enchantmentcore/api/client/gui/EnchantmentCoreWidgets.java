package johnsmith.enchantmentcore.api.client.gui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.factory.WidgetFactory;
import johnsmith.configoverhauled.api.client.gui.registry.WidgetRegistry;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreenFactory;

import johnsmith.enchantmentcore.client.gui.entry.EnchantableListEntry;
import johnsmith.enchantmentcore.client.gui.entry.SlotListEntry;
import johnsmith.enchantmentcore.config.property.EnchantableItemListProperty;
import johnsmith.enchantmentcore.config.property.SlotListProperty;

import net.minecraft.client.gui.screens.Screen;

/**
 * Public client GUI API providing Enchantment Core's custom {@link Property}-to-{@link WidgetFactory}
 * mappings for integration with Config Overhauled configuration screens.
 * <p>
 * Dependent mods that utilize Enchantment Core's dynamic enchantment properties (such as
 * {@link EnchantableItemListProperty} and {@link SlotListProperty}) can use this utility to register
 * the corresponding interactive selection widgets onto their own {@link ConfigScreen} or {@link WidgetRegistry}.
 */
public final class EnchantmentCoreWidgets {
    /** Unmodifiable map holding the registered property class to widget factory bindings. */
    private static final Map<Class<? extends Property<?>>, WidgetFactory<?>> WIDGET_FACTORIES;

    static {
        Map<Class<? extends Property<?>>, WidgetFactory<?>> map = new LinkedHashMap<>();
        putFactory(map, EnchantableItemListProperty.class, EnchantableListEntry::new);
        putFactory(map, SlotListProperty.class, SlotListEntry::new);
        WIDGET_FACTORIES = Collections.unmodifiableMap(map);
    }

    /** Private constructor to prevent instantiation of this utility class. */
    private EnchantmentCoreWidgets() {}

    /**
     * Type-safe helper method for inserting a property-to-widget factory binding into the internal map.
     *
     * @param map           The destination map receiving the factory entry.
     * @param propertyClass The class token of the custom {@link Property} implementation.
     * @param factory       The {@link WidgetFactory} constructor bound to the property type.
     * @param <P>           The concrete property type.
     */
    private static <P extends Property<?>> void putFactory(
            Map<Class<? extends Property<?>>, WidgetFactory<?>> map,
            Class<P> propertyClass,
            WidgetFactory<? super P> factory
    ) {
        map.put(propertyClass, factory);
    }

    /**
     * Retrieves an unmodifiable view of Enchantment Core's custom property-to-widget factory mappings.
     *
     * @return A map binding custom {@link Property} classes to their corresponding {@link WidgetFactory} constructors.
     */
    public static Map<Class<? extends Property<?>>, WidgetFactory<?>> getWidgetFactories() {
        return WIDGET_FACTORIES;
    }

    /**
     * Retrieves the {@link WidgetFactory} registered by Enchantment Core for a specific property class.
     *
     * @param propertyClass The target property class token.
     * @param <P>           The concrete property type.
     * @return The bound {@link WidgetFactory}, or {@code null} if the class is not a custom Enchantment Core property.
     */
    @SuppressWarnings("unchecked")
    public static <P extends Property<?>> WidgetFactory<P> getWidgetFactory(Class<P> propertyClass) {
        return (WidgetFactory<P>) WIDGET_FACTORIES.get(propertyClass);
    }

    /**
     * Registers all of Enchantment Core's custom property-to-widget mappings directly into the provided {@link ConfigScreen}.
     *
     * @param screen The target configuration screen instance to populate.
     * @param <S>    The concrete configuration screen type.
     * @return The populated configuration screen instance for fluent chaining.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S extends ConfigScreen> S register(S screen) {
        WIDGET_FACTORIES.forEach((propClass, factory) ->
                screen.registerWidget((Class) propClass, (WidgetFactory) factory)
        );
        return screen;
    }

    /**
     * Registers all of Enchantment Core's custom property-to-widget mappings into the provided {@link WidgetRegistry}.
     *
     * @param registry The target widget registry to populate.
     * @param <R>      The concrete widget registry type.
     * @return The populated widget registry instance for fluent chaining.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <R extends WidgetRegistry> R register(R registry) {
        WIDGET_FACTORIES.forEach((propClass, factory) ->
                registry.register((Class) propClass, (WidgetFactory) factory)
        );
        return registry;
    }

    /**
     * Creates a screen factory function suitable for {@link ConfigManager#setScreenFactory(Function)} that constructs
     * a default {@link ConfigScreen} pre-populated with Enchantment Core's custom widget mappings.
     *
     * @param manager The target mod's {@link ConfigManager}.
     * @return A screen factory function accepting a parent {@link Screen} and returning a configured {@link ConfigScreen}.
     */
    public static Function<Object, Object> createScreenFactory(ConfigManager manager) {
        return parent -> register(ConfigScreenFactory.createDefault((Screen) parent, manager));
    }
}