package johnsmith.enchantmentcore;

import johnsmith.enchantmentcore.api.config.ModLoadEvaluator;
import johnsmith.enchantmentcore.api.config.OrphanHandler;
import johnsmith.enchantmentcore.api.tag.DamageTypes;
import johnsmith.enchantmentcore.api.tag.ItemTags;
import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.config.Orphanage;
import johnsmith.enchantmentcore.platform.Services;
import johnsmith.enchantmentcore.registry.DataTransformerRegistry;
import johnsmith.enchantmentcore.registry.EnchantmentCoreRegistries;

public class Common {
    public static void initialize() {
        OrphanHandler.Provider.set(new Orphanage());
        ModLoadEvaluator.Provider.set(Services.PLATFORM::isModLoaded);

        EnchantmentCoreRegistries.initialize();
        DamageTypes.initialize();
        ItemTags.initialize();

        DataTransformerRegistry.registerTypeExclusions(Config.USER_TYPE_EXCLUSIONS.get());
        DataTransformerRegistry.registerStructuralExclusions(Config.USER_STRUCTURAL_EXCLUSIONS.get());
    }
}
