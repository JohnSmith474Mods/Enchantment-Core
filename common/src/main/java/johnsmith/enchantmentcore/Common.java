package johnsmith.enchantmentcore;

import johnsmith.enchantmentcore.api.config.ModLoadEvaluator;
import johnsmith.enchantmentcore.api.config.OrphanHandler;
import johnsmith.enchantmentcore.api.registry.DataTransformerRegistry;
import johnsmith.enchantmentcore.api.registry.EnchantmentCoreRegistries;
import johnsmith.enchantmentcore.api.tag.DamageTypes;
import johnsmith.enchantmentcore.api.tag.ItemTags;
import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.config.Orphanage;
import johnsmith.enchantmentcore.config.registry.EnchantmentCore$DynamicPropertyTypeRegistry;

/**
 * The common entry point for Enchantment Core.
 * <p>
 * Initializes platform-agnostic providers and static {@link net.minecraft.resources.ResourceKey}
 * definitions without mutating frozen vanilla {@link net.minecraft.core.registries.BuiltInRegistries}
 * during class loading.
 */
public final class Common {

    public static void initialize(ModLoadEvaluator modLoadEvaluator) {
        OrphanHandler.Provider.set(new Orphanage());
        ModLoadEvaluator.Provider.set(modLoadEvaluator);
        EnchantmentCore$DynamicPropertyTypeRegistry.initialize();
        EnchantmentCoreRegistries.initialize();
        DamageTypes.initialize();
        ItemTags.initialize();
        DataTransformerRegistry.registerDynamicTypeExclusions(Config.USER_TYPE_EXCLUSIONS::get);
        DataTransformerRegistry.registerDynamicStructuralExclusions(Config.USER_STRUCTURAL_EXCLUSIONS::get);
    }

    private Common() {}
}