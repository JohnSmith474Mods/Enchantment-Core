package johnsmith.enchantmentcore.enchantment.spellfield.effect.volume;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVolumeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import johnsmith.enchantmentcore.enchantment.spellfield.effect.ProbabilityDistribution;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public record SurfaceHazardEffect(
        EntityType<?> entityType,
        LevelBasedValue count,
        LevelBasedValue verticalSearch,
        Optional<ProbabilityDistribution> distribution,
        Optional<ContextAwarePredicate> entityFilter
) implements SpellFieldEntityEffect, SpellFieldVolumeEffect {

    public static final String ENTITY_TYPE = "entity_type";
    public static final String COUNT = "count";
    public static final String VERTICAL_SEARCH = "vertical_search";
    public static final String DISTRIBUTION = "distribution";
    public static final String ENTITY_FILTER = "entity_filter";

    public static final String KEY = "surface_hazard";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(COUNT, VERTICAL_SEARCH);

    public static final MapCodec<SurfaceHazardEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf(ENTITY_TYPE).forGetter(SurfaceHazardEffect::entityType),
            LevelBasedValue.CODEC.optionalFieldOf(COUNT, LevelBasedValue.constant(1.0F)).forGetter(SurfaceHazardEffect::count),
            LevelBasedValue.CODEC.optionalFieldOf(VERTICAL_SEARCH, LevelBasedValue.constant(5.0F)).forGetter(SurfaceHazardEffect::verticalSearch),
            ProbabilityDistribution.CODEC.optionalFieldOf(DISTRIBUTION).forGetter(SurfaceHazardEffect::distribution),
            ContextAwarePredicate.CODEC.optionalFieldOf(ENTITY_FILTER).forGetter(SurfaceHazardEffect::entityFilter)
    ).apply(instance, SurfaceHazardEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float volumeScalar) {
        if (this.entityFilter.isPresent() && !this.entityFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, victim, enchantmentLevel))) return;
        this.dispatchHazard(level, enchantmentLevel, context.owner(), victim.position(), volumeScalar);
    }

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, List<GlobalVolume> volumes) {
        if (volumes.isEmpty()) return;

        int totalHazards = (int) this.count.calculate(enchantmentLevel);
        for (int i = 0; i < totalHazards; i++) {
            Vec3 targetPoint = this.distribution
                    .map(d -> d.samplePoint(level, enchantmentLevel, spatialReference, epicenter, volumes))
                    .orElseGet(() -> ProbabilityDistribution.getUniformPoint(volumes, level.random));

            this.dispatchHazard(level, enchantmentLevel, context.owner(), targetPoint, 1.0F);
        }
    }

    private void dispatchHazard(ServerLevel level, int enchantmentLevel, LivingEntity owner, Vec3 targetPos, float scalar) {
        if (scalar <= 0.0001F) return;

        int range = (int) this.verticalSearch.calculate(enchantmentLevel);
        BlockPos.MutableBlockPos cursor = BlockPos.containing(targetPos).mutable();
        cursor.setY(cursor.getY() + range);

        boolean found = false;
        for (int y = 0; y <= range * 2; y++) {
            BlockState state = level.getBlockState(cursor);
            VoxelShape collision = state.getCollisionShape(level, cursor);

            if (!collision.isEmpty()) {
                found = true;
                break;
            }
            cursor.move(0, -1, 0);
        }

        if (found) {
            Entity hazard = this.entityType.create(level, EntitySpawnReason.TRIGGERED);
            if (hazard != null) {
                hazard.setPos(cursor.getX() + 0.5D, cursor.getY() + 1.0D, cursor.getZ() + 0.5D);

                if (owner != null) {
                    if (hazard instanceof Projectile projectile) {
                        projectile.setOwner(owner);
                    } else if (hazard instanceof EvokerFangs fangs) {
                        fangs.setOwner(owner);
                    }
                }

                level.addFreshEntity(hazard);
            }
        }
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() {
        return CODEC;
    }
}