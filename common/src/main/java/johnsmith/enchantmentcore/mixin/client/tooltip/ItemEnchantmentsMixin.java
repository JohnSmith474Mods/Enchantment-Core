package johnsmith.enchantmentcore.mixin.client.tooltip;

import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.api.tooltip.TooltipFormatterRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

/**
 * Mixin targeting the item enchantments data component.
 * Intercepts tooltip construction to delegate formatting to the custom formatter registry.
 */
@Mixin(ItemEnchantments.class)
public abstract class ItemEnchantmentsMixin {

    @Shadow public abstract Set<Object2IntMap.Entry<Holder<Enchantment>>> entrySet();

    /**
     * Intercepts the addition of enchantment names to an item's tooltip.
     * Bypasses the vanilla single-line logic to execute registered multi-line formatters.
     *
     * @param context             The tooltip generation context.
     * @param tooltipAdder        The consumer accepting the generated text components.
     * @param tooltipFlag         The active tooltip flag determining verbosity.
     * @param dataComponentGetter The provider for adjacent item data components.
     * @param ci                  The callback information used to cancel the vanilla execution.
     */
    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void onAddToTooltip(Item.TooltipContext context, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter, CallbackInfo ci) {
        if (!Config.TOOLTIP_FORMATTING.get()) {
            return;
        }

        for (var entry : this.entrySet()) {
            List<Component> components = TooltipFormatterRegistry.getFormatted(entry.getKey(), entry.getIntValue());
            components.forEach(tooltipAdder);
        }

        ci.cancel();
    }
}