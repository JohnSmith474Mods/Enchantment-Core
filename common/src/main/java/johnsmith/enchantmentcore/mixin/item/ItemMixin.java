package johnsmith.enchantmentcore.mixin.item;

import johnsmith.enchantmentcore.config.Config;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin targeting the base Item class to override intrinsic enchantability constraints.
 * Allows items with zero default enchantability to receive enchantments via the enchanting table.
 */
@Mixin(Item.class)
public class ItemMixin {

    /**
     * Intercepts the enchantability value retrieval.
     * Replaces non-enchantable states (0) with the configured global baseline value.
     *
     * @param cir The callback information storing the return value.
     */
    @Inject(method = "getEnchantmentValue", at = @At("RETURN"), cancellable = true)
    private void onGetEnchantmentValue(CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() <= 0) {
            cir.setReturnValue(Config.BASE_ENCHANTABILITY.get());
        }
    }
}