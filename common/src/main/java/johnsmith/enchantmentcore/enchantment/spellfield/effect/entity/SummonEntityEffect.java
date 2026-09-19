package johnsmith.enchantmentcore.enchantment.spellfield.effect.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record SummonEntityEffect(
        EntityType<?> entityType,
        LevelBasedValue count,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect {

    public static final String ENTITY_TYPE = "entity_type";
    public static final String COUNT = "count";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "summon_entity";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(COUNT);

    public static final MapCodec<SummonEntityEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf(ENTITY_TYPE).forGetter(SummonEntityEffect::entityType),
            LevelBasedValue.CODEC.optionalFieldOf(COUNT, LevelBasedValue.constant(1.0F)).forGetter(SummonEntityEffect::count),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(SummonEntityEffect::entityFilter)
    ).apply(instance, SummonEntityEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;

        float calculatedCount = this.count.calculate(enchantmentLevel) * volumeScalar;
        int spawnAmount = (int) calculatedCount;

        if (level.random.nextFloat() < (calculatedCount - spawnAmount)) {
            spawnAmount++;
        }

        for (int i = 0; i < spawnAmount; i++) {
            Entity spawned = this.entityType.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (spawned != null) {
                spawned.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), victim.getXRot());
                level.addFreshEntity(spawned);
            }
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }
}