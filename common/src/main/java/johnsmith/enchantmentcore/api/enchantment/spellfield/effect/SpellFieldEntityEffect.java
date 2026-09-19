package johnsmith.enchantmentcore.api.enchantment.spellfield.effect;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Defines an effect applied to individual entities within a spell field volume.
 */
public interface SpellFieldEntityEffect extends SpellFieldEffect {

    /**
     * Executes the entity modification logic on the specified victim.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param victim           The entity targeted by the spell field.
     * @param volumeScalar     The active topological multiplier for the target.
     */
    void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar);

    /**
     * Constructs a standard loot context for evaluating entity predicates and loot tables.
     *
     * @param level            The executing server level.
     * @param target           The target entity for context evaluation.
     * @param enchantmentLevel The level of the spell field enchantment.
     * @return The configured loot context.
     */
    static LootContext createLootContext(ServerLevel level, Entity target, int enchantmentLevel) {
        return new LootContext.Builder(
                new LootParams.Builder(level)
                        .withParameter(LootContextParams.THIS_ENTITY, target)
                        .withParameter(LootContextParams.ORIGIN, target.position())
                        .withParameter(LootContextParams.ENCHANTMENT_LEVEL, enchantmentLevel)
                        .create(LootContextParamSets.ENCHANTED_ENTITY)
        ).create(Optional.empty());
    }
}