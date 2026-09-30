package johnsmith.enchantmentcore.mixin.client.tooltip;

import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.api.tooltip.TooltipFormatterRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

/**
 * Mixin targeting the ItemStack tooltip rendering.
 * Safely intercepts the rendering of both APPLIED and STORED enchantments to delegate to the registry.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V", at = @At("HEAD"), cancellable = true)
    private <T extends TooltipProvider> void enchantment_core$interceptEnchantmentTooltip(
            DataComponentType<T> componentType, Item.TooltipContext context, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag, CallbackInfo ci) {

        if (!Config.TOOLTIP_FORMATTING.get()) return;

        if (componentType == DataComponents.ENCHANTMENTS || componentType == DataComponents.STORED_ENCHANTMENTS) {
            ItemStack self = (ItemStack) (Object) this;
            ItemEnchantments enchantments = self.get((DataComponentType<ItemEnchantments>) componentType);

            // Cast the component to our Accessor to read the package-private field
            if (enchantments != null && ((ItemEnchantmentsAccessor) enchantments).enchantment_core$showInTooltip()) {

                for (var entry : enchantments.entrySet()) {
                    List<Component> components = TooltipFormatterRegistry.getFormatted(entry.getKey(), entry.getIntValue(), self);
                    components.forEach(tooltipAdder);
                }
            }

            // Halt vanilla processing so it doesn't double-render the enchantments
            ci.cancel();
        }
    }
}