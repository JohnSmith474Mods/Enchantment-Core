package johnsmith.enchantmentcore.mixin.enchantment.effect.entity.projectile;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;
import java.util.Optional;

import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import org.apache.commons.lang3.mutable.MutableFloat;

import org.jetbrains.annotations.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin modifying the initial flight trajectory divergence of spawned projectiles.
 * Evaluates and applies the {@code PROJECTILE_ACCURACY} enchantment component to the base inaccuracy parameter.
 */
@Mixin(Projectile.class)
public abstract class AccuracyMixin {

    @Shadow @Nullable public abstract Entity getOwner();

    /**
     * Intercepts the {@code inaccuracy} float variable directly within the {@code shoot} method arguments.
     * Iterates through the shooter's active weapon enchantments to calculate compounded accuracy modifications.
     *
     * @param inaccuracy The base vanilla inaccuracy value.
     * @return The modified inaccuracy value, clamped at 0.0 to prevent inverted trajectory vectors.
     */
    @ModifyVariable(method = "shoot(DDDFF)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private float enchantment_core$modifyProjectileInaccuracy(float inaccuracy) {
        Entity owner = this.getOwner();
        if (owner instanceof LivingEntity living) {
            ItemStack weapon = living.getUseItem();
            if (weapon.isEmpty()) {
                weapon = living.getMainHandItem();
            }

            if (!weapon.isEmpty()) {
                ItemEnchantments enchantments = weapon.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

                if (!enchantments.isEmpty()) {
                    MutableFloat modifiedInaccuracy = new MutableFloat(inaccuracy);
                    boolean isServer = living.level() instanceof ServerLevel;

                    for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                        Holder<Enchantment> enchantment = entry.getKey();
                        int level = entry.getIntValue();

                        List<ConditionalEffect<EnchantmentValueEffect>> accuracyEffects = enchantment.value().effects().get(EnchantmentEffectComponentRegistry.PROJECTILE_ACCURACY);

                        if (accuracyEffects != null) {
                            LootContext lootContext = null;

                            // Instantiate loot context only on the logical server.
                            if (isServer) {
                                LootParams params = new LootParams.Builder((ServerLevel) living.level())
                                        .withParameter(LootContextParams.THIS_ENTITY, living)
                                        .withParameter(LootContextParams.ENCHANTMENT_LEVEL, level)
                                        .withParameter(LootContextParams.ORIGIN, living.position())
                                        .create(LootContextParamSets.ENCHANTED_ENTITY);
                                lootContext = new LootContext.Builder(params).create(Optional.empty());
                            }

                            for (ConditionalEffect<EnchantmentValueEffect> conditional : accuracyEffects) {
                                // Allow clients to blindly predict accuracy modifications to prevent visual desync.
                                if (!isServer || conditional.matches(lootContext)) {
                                    modifiedInaccuracy.setValue(conditional.effect().process(level, living.getRandom(), modifiedInaccuracy.getValue()));
                                }
                            }
                        }
                    }

                    // Clamp to 0.0f to prevent negative divergence, which flips vectors backward.
                    return Math.max(0.0f, modifiedInaccuracy.getValue());
                }
            }
        }
        return inaccuracy;
    }
}