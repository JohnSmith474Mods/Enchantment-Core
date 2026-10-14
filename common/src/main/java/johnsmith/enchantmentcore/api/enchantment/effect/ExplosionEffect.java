package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.phys.Vec3;

/**
 * Dual-purpose enchantment effect implementing both {@link EnchantmentEntityEffect} and
 * {@link EnchantmentLocationBasedEffect} that triggers an explosion defined by an {@link ExplosionDefinition}
 * when a random roll succeeds against {@link #chance()}.
 *
 * @param chance    The {@link LevelBasedValue} governing the probability (0.0 to 1.0) of triggering the explosion.
 * @param explosion The {@link ExplosionDefinition} specifying radius, block interaction, fire, and particle parameters.
 */
public record ExplosionEffect(
        LevelBasedValue chance,
        ExplosionDefinition explosion
) implements EnchantmentEntityEffect, EnchantmentLocationBasedEffect {
    /** JSON field key for the explosion trigger probability. */
    public static final String CHANCE = "chance";

    /** JSON field key for the nested explosion definition object. */
    public static final String EXPLOSION = "explosion";

    /** Registry path identifier for this effect. */
    public static final String KEY = "explode";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation,
     * combining {@link #CHANCE} with the keys of {@link ExplosionDefinition#KEY_PROVIDER}.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> {
        Set<String> keys = new HashSet<>(ExplosionDefinition.KEY_PROVIDER.getLevelBasedKeys());
        keys.add(CHANCE);
        return keys;
    };

    /**
     * MapCodec for serializing and deserializing {@link ExplosionEffect} instances from JSON.
     */
    public static final MapCodec<ExplosionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.optionalFieldOf(CHANCE, LevelBasedValue.constant(1.0F)).forGetter(ExplosionEffect::chance),
            ExplosionDefinition.CODEC.fieldOf(EXPLOSION).forGetter(ExplosionEffect::explosion)
    ).apply(instance, ExplosionEffect::new));

    /**
     * Evaluates the trigger probability and detonates the configured explosion at {@code origin}.
     *
     * @param level            The server level in which the effect is triggered.
     * @param enchantmentLevel The level of the source enchantment.
     * @param item             The context of the enchanted item in use.
     * @param entity           The target or context entity.
     * @param origin           The world coordinate where the explosion is centered.
     */
    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 origin) {
        float probability = this.chance.calculate(enchantmentLevel);
        if (probability >= 1.0F || level.getRandom().nextFloat() < probability) {
            this.explosion.explode(level, enchantmentLevel, item.owner(), origin, 1.0F);
        }
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}