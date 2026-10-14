package johnsmith.enchantmentcore.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.PowderSnowBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Injects logic into the powder snow block to evaluate custom walkability conditions.
 * Grants entities the ability to traverse powder snow without sinking if they possess
 * an equipped item containing the powder snow walkable enchantment component.
 */
@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {

    /**
     * Modifies the return value of the entity walkability check.
     * Iterates through all equipment slots of the entity. Overrides a negative original result
     * if an item with the relevant component is found.
     *
     * @param original The original boolean result determined by vanilla logic.
     * @param entity   The entity attempting to walk on the powder snow block.
     * @return True if the entity has the required component or the original logic permits walking. False otherwise.
     */
    @ModifyReturnValue(method = "canEntityWalkOnPowderSnow", at = @At("RETURN"))
    private static boolean enchantment_core$canEntityWalkOnPowderSnow(final boolean original, @Local(argsOnly = true) Entity entity) {
        if (!original && entity instanceof LivingEntity living) {
            for (ItemStack stack : living.getAllSlots()) {
                if (!stack.isEmpty() && EnchantmentHelper.has(stack, EnchantmentEffectComponentRegistry.POWDER_SNOW_WALKABLE)) {
                    return true;
                }
            }
        }
        return original;
    }
}