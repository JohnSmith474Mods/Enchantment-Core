package johnsmith.enchantmentcore.enchantment.effect;

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

public record ExplosionEffect(
        LevelBasedValue chance,
        ExplosionDefinition explosion
) implements EnchantmentEntityEffect, EnchantmentLocationBasedEffect {

    public static final String CHANCE = "chance";
    public static final String EXPLOSION = "explosion";

    public static final String KEY = "explode";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> {
        Set<String> keys = new HashSet<>(ExplosionDefinition.KEY_PROVIDER.getLevelBasedKeys());
        keys.add(CHANCE);
        return keys;
    };

    public static final MapCodec<ExplosionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.optionalFieldOf(CHANCE, LevelBasedValue.constant(1.0F)).forGetter(ExplosionEffect::chance),
            ExplosionDefinition.CODEC.fieldOf(EXPLOSION).forGetter(ExplosionEffect::explosion)
    ).apply(instance, ExplosionEffect::new));

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