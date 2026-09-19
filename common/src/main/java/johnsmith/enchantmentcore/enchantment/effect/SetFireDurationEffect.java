package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.entity.accessor.FireDurationAccessor;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

public record SetFireDurationEffect(
        LevelBasedValue duration
) implements EnchantmentEntityEffect {

    public static final String DURATION = "duration";

    public static final String KEY = "set_fire_duration";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(DURATION);

    public static final MapCodec<SetFireDurationEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(DURATION).forGetter(SetFireDurationEffect::duration)
    ).apply(instance, SetFireDurationEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity entity, Vec3 origin) {
        if (entity instanceof FireDurationAccessor accessor) {
            float seconds = this.duration.calculate(enchantmentLevel);
            accessor.enchantment_core$setFireDuration(seconds);
        }
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}