package johnsmith.enchantmentcore.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record RandomTeleportEffect(
        LevelBasedValue range,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {

    public static final String RANGE = "range";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "random_teleport";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(RANGE);

    public static final MapCodec<RandomTeleportEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(RANGE).forGetter(RandomTeleportEffect::range),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(RandomTeleportEffect::entityFilter)
    ).apply(instance, RandomTeleportEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (!(victim instanceof LivingEntity living)) return;
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        double tpRange = this.range.calculate(enchantmentLevel);

        for (int i = 0; i < 16; i++) {
            double x = living.getX() + (living.getRandom().nextDouble() - 0.5D) * tpRange * 2.0D;
            double y = Mth.clamp(living.getY() + (living.getRandom().nextInt((int) tpRange * 2) - tpRange), level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
            double z = living.getZ() + (living.getRandom().nextDouble() - 0.5D) * tpRange * 2.0D;

            if (living.isPassenger()) living.stopRiding();

            Vec3 oldPos = living.position();
            if (living.randomTeleport(x, y, z, true)) {
                level.playSound(null, oldPos.x, oldPos.y, oldPos.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                living.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
                break;
            }
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() { return CODEC; }
}