package johnsmith.enchantmentcore.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import johnsmith.enchantmentcore.api.registry.EnchantmentEffectComponents;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.PowderSnowBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {

    @Unique
    private static final EquipmentSlot[] enchantment_core$SLOTS = EquipmentSlot.values();

    @ModifyReturnValue(method = "canEntityWalkOnPowderSnow", at = @At("RETURN"))
    private static boolean enchantment_core$canEntityWalkOnPowderSnow(final boolean original, @Local(argsOnly = true) Entity entity) {
        if (original) {
            return true;
        }

        if (entity instanceof LivingEntity living) {
            for (EquipmentSlot enchantmentCore$SLOT : enchantment_core$SLOTS) {
                ItemStack stack = living.getItemBySlot(enchantmentCore$SLOT);
                if (!stack.isEmpty() && EnchantmentHelper.has(stack, EnchantmentEffectComponents.POWDER_SNOW_WALKABLE)) {
                    return true;
                }
            }
        }

        return false;
    }
}