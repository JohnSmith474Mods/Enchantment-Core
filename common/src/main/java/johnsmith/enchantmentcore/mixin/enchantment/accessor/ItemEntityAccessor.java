package johnsmith.enchantmentcore.mixin.enchantment.accessor;

import net.minecraft.world.entity.item.ItemEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin accessor interface targeting the {@link ItemEntity} class.
 * Exposes the internal age timer to permit direct modification of the entity despawn cycle.
 */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {

    /**
     * Modifies the internal age variable of the item entity.
     * Assigning a value of 0 resets the despawn timer, extending the entity lifetime.
     *
     * @param age The new age value in ticks.
     */
    @Accessor("age")
    void enchantment_core$setAge(int age);
}