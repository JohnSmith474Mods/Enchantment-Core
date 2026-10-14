package johnsmith.enchantmentcore.api.enchantment.effect;

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

/**
 * An {@link EnchantmentEntityEffect} that assigns a custom burn duration (in seconds) to an entity
 * implementing {@link FireDurationAccessor}.
 *
 * @param duration The {@link LevelBasedValue} specifying the fire duration in seconds.
 */
public record SetFireDurationEffect(
        LevelBasedValue duration
) implements EnchantmentEntityEffect {
    /** JSON field key for the fire duration in seconds. */
    public static final String DURATION = "duration";

    /** Registry path identifier for this entity effect. */
    public static final String KEY = "set_fire_duration";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(DURATION);

    /**
     * MapCodec for serializing and deserializing {@link SetFireDurationEffect} instances from JSON.
     */
    public static final MapCodec<SetFireDurationEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(DURATION).forGetter(SetFireDurationEffect::duration)
    ).apply(instance, SetFireDurationEffect::new));

    /**
     * Assigns the evaluated fire duration in seconds to {@code entity} if it implements {@link FireDurationAccessor}.
     *
     * @param level            The server level in which the effect is triggered.
     * @param enchantmentLevel The level of the source enchantment.
     * @param context          The context of the enchanted item in use.
     * @param entity           The target entity receiving the fire duration state.
     * @param origin           The origin position vector of the trigger.
     */
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