package johnsmith.enchantmentcore.api.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reusable data-driven configuration record describing the parameters of an explosion triggered by
 * {@link ExplosionEffect} or {@link johnsmith.enchantmentcore.api.enchantment.spellfield.effect.volume.ExplodeEffect}.
 *
 * @param radius         The {@link LevelBasedValue} governing the base blast radius of the explosion.
 * @param createFire     Whether the explosion ignites fires on surrounding blocks.
 * @param damageEntities Whether the explosion inflicts blast damage to nearby entities.
 * @param interaction    The block destruction behavior of the explosion ({@link ExplosionInteraction}).
 * @param immuneFilter   An optional {@link ContextAwarePredicate} identifying entities immune to blast damage from this explosion.
 * @param smallParticles Optional custom particle options for minor blast particles.
 * @param largeParticles Optional custom particle options for major emitter blast particles.
 * @param sound          Optional custom sound event played upon detonation.
 */
public record ExplosionDefinition(
        LevelBasedValue radius,
        boolean createFire,
        boolean damageEntities,
        ExplosionInteraction interaction,
        Optional<ContextAwarePredicate> immuneFilter,
        Optional<ParticleOptions> smallParticles,
        Optional<ParticleOptions> largeParticles,
        Optional<Holder<SoundEvent>> sound
) {
    /** JSON field key for the explosion radius. */
    public static final String RADIUS = "radius";

    /** JSON field key for toggling fire creation. */
    public static final String CREATE_FIRE = "create_fire";

    /** JSON field key for toggling entity blast damage. */
    public static final String DAMAGE_ENTITIES = "damage_entities";

    /** JSON field key for the block interaction type. */
    public static final String EXPLOSION_INTERACTION = "interaction";

    /** JSON field key for the entity immunity predicate filter. */
    public static final String IMMUNE_FILTER = "immune_filter";

    /** JSON field key for custom small explosion particles. */
    public static final String SMALL_PARTICLES = "small_particles";

    /** JSON field key for custom large explosion emitter particles. */
    public static final String LARGE_PARTICLES = "large_particles";

    /** JSON field key for the custom detonation sound event. */
    public static final String SOUND = "sound";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(RADIUS);

    /**
     * Codec for serializing and deserializing {@link ExplosionDefinition} instances from JSON.
     */
    public static final Codec<ExplosionDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf(RADIUS).forGetter(ExplosionDefinition::radius),
            Codec.BOOL.optionalFieldOf(CREATE_FIRE, false).forGetter(ExplosionDefinition::createFire),
            Codec.BOOL.optionalFieldOf(DAMAGE_ENTITIES, true).forGetter(ExplosionDefinition::damageEntities),
            ExplosionInteraction.CODEC.optionalFieldOf(EXPLOSION_INTERACTION, ExplosionInteraction.BLOCK).forGetter(ExplosionDefinition::interaction),
            ContextAwarePredicate.CODEC.optionalFieldOf(IMMUNE_FILTER).forGetter(ExplosionDefinition::immuneFilter),
            ParticleTypes.CODEC.optionalFieldOf(SMALL_PARTICLES).forGetter(ExplosionDefinition::smallParticles),
            ParticleTypes.CODEC.optionalFieldOf(LARGE_PARTICLES).forGetter(ExplosionDefinition::largeParticles),
            BuiltInRegistries.SOUND_EVENT.holderByNameCodec().optionalFieldOf(SOUND).forGetter(ExplosionDefinition::sound)
    ).apply(instance, ExplosionDefinition::new));

    /**
     * Detonates an explosion in the specified server level using this definition's parameters.
     *
     * @param level            The server level where the explosion occurs.
     * @param enchantmentLevel The active level of the enchantment, used to evaluate {@link #radius()}.
     * @param source           The entity attributed as the cause of the explosion, or {@code null}.
     * @param pos              The world coordinates of the explosion center.
     * @param scalar           A topological multiplier applied to the calculated explosion radius.
     */
    public void explode(ServerLevel level, int enchantmentLevel, Entity source, Vec3 pos, float scalar) {
        float calculatedRadius = this.radius.calculate(enchantmentLevel) * scalar;
        if (calculatedRadius <= 0.0F) return;

        ExplosionDamageCalculator damageCalculator = createExplosionDamageCalculator(level, enchantmentLevel);
        DamageSource damageSource = level.damageSources().explosion(source, source);
        Level.ExplosionInteraction vanillaInteraction = getVanillaInteraction(this.interaction);

        if (this.smallParticles.isPresent() || this.largeParticles.isPresent() || this.sound.isPresent()) {
            ParticleOptions small = this.smallParticles.orElse(ParticleTypes.EXPLOSION);
            ParticleOptions large = this.largeParticles.orElse(ParticleTypes.EXPLOSION_EMITTER);
            Holder<SoundEvent> snd = this.sound.orElse(SoundEvents.GENERIC_EXPLODE);

            level.explode(
                    source,
                    damageSource,
                    damageCalculator,
                    pos.x(),
                    pos.y(),
                    pos.z(),
                    calculatedRadius,
                    this.createFire,
                    vanillaInteraction,
                    small,
                    large,
                    WeightedList.<ExplosionParticleInfo>builder()
                            .add(new ExplosionParticleInfo(ParticleTypes.POOF, 0.5F, 1.0F))
                            .add(new ExplosionParticleInfo(ParticleTypes.SMOKE, 1.0F, 1.0F))
                            .build(),
                    snd
            );
        } else {
            level.explode(source, damageSource, damageCalculator, pos.x(), pos.y(), pos.z(), calculatedRadius, this.createFire, vanillaInteraction);
        }
    }

    /**
     * Creates a custom {@link ExplosionDamageCalculator} if entity damage is disabled or if an
     * {@link #immuneFilter()} predicate is present.
     *
     * @param level            The active server level.
     * @param enchantmentLevel The active level of the enchantment.
     * @return A custom {@link ExplosionDamageCalculator}, or {@code null} to use default vanilla damage calculation.
     */
    private @Nullable ExplosionDamageCalculator createExplosionDamageCalculator(ServerLevel level, int enchantmentLevel) {
        ExplosionDamageCalculator damageCalculator = null;
        if (!this.damageEntities) {
            damageCalculator = new ExplosionDamageCalculator() {
                @Override
                public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                    return false;
                }
            };
        } else if (this.immuneFilter.isPresent()) {
            damageCalculator = new ExplosionDamageCalculator() {
                @Override
                public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                    if (immuneFilter.get().matches(SpellFieldEntityEffect.createLootContext(level, entity, enchantmentLevel))) {
                        return false;
                    }
                    return super.shouldDamageEntity(explosion, entity);
                }
            };
        }
        return damageCalculator;
    }

    /**
     * Maps an {@link ExplosionInteraction} constant to its corresponding vanilla {@link Level.ExplosionInteraction}.
     *
     * @param interaction The Enchantment Core explosion interaction constant.
     * @return The corresponding vanilla {@link Level.ExplosionInteraction}.
     */
    public static Level.ExplosionInteraction getVanillaInteraction(ExplosionInteraction interaction) {
        return switch (interaction) {
            case NONE -> Level.ExplosionInteraction.NONE;
            case BLOCK -> Level.ExplosionInteraction.BLOCK;
            case MOB -> Level.ExplosionInteraction.MOB;
            case TNT -> Level.ExplosionInteraction.TNT;
            case TRIGGER -> Level.ExplosionInteraction.TRIGGER;
        };
    }

    /**
     * Serializable representation of vanilla {@link Level.ExplosionInteraction} block interaction behaviors.
     */
    public enum ExplosionInteraction implements StringRepresentable {
        /** No blocks are destroyed or triggered. */
        NONE("none"),
        /** Standard block destruction respecting block drop rules. */
        BLOCK("block"),
        /** Mob griefing dependent block destruction. */
        MOB("mob"),
        /** TNT-style block destruction with full item drop rates. */
        TNT("tnt"),
        /** Triggers redstone-sensitive blocks (like wind charges) without destroying blocks. */
        TRIGGER("trigger");

        /**
         * Codec for serializing and deserializing {@link ExplosionInteraction} constants by their lowercase identifier.
         */
        public static final Codec<ExplosionInteraction> CODEC = StringRepresentable.fromEnum(ExplosionInteraction::values);

        /** The serialized string identifier of this interaction mode. */
        private final String id;

        ExplosionInteraction(String id) {
            this.id = id;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.id;
        }
    }

    // region Wither Pattern

    /**
     * Constructs a default {@link ExplosionDefinition} (radius 1, fire enabled, entity damage enabled, TNT interaction).
     */
    public ExplosionDefinition() {
        this(LevelBasedValue.constant(1), true, true, ExplosionInteraction.TNT, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * Returns a copy of this definition with the specified radius.
     *
     * @param radius The new {@link LevelBasedValue} radius.
     * @return A new {@link ExplosionDefinition} instance.
     */
    public ExplosionDefinition withRadius(LevelBasedValue radius) {
        return new ExplosionDefinition(radius, createFire, damageEntities, interaction, immuneFilter, smallParticles, largeParticles, sound);
    }

    /**
     * Returns a copy of this definition with the specified fire creation flag.
     *
     * @param createFire Whether the explosion should create fire.
     * @return A new {@link ExplosionDefinition} instance.
     */
    public ExplosionDefinition withCreateFire(boolean createFire) {
        return new ExplosionDefinition(radius, createFire, damageEntities, interaction, immuneFilter, smallParticles, largeParticles, sound);
    }

    /**
     * Returns a copy of this definition with the specified entity damage flag.
     *
     * @param damageEntities Whether the explosion should damage entities.
     * @return A new {@link ExplosionDefinition} instance.
     */
    public ExplosionDefinition withDamageEntities(boolean damageEntities) {
        return new ExplosionDefinition(radius, createFire, damageEntities, interaction, immuneFilter, smallParticles, largeParticles, sound);
    }

    /**
     * Returns a copy of this definition with the specified block interaction mode.
     *
     * @param interaction The new {@link ExplosionInteraction} mode.
     * @return A new {@link ExplosionDefinition} instance.
     */
    public ExplosionDefinition withInteraction(ExplosionInteraction interaction) {
        return new ExplosionDefinition(radius, createFire, damageEntities, interaction, immuneFilter, smallParticles, largeParticles, sound);
    }

    /**
     * Returns a copy of this definition with the specified entity immunity predicate.
     *
     * @param immuneFilter The predicate matching entities immune to this explosion.
     * @return A new {@link ExplosionDefinition} instance.
     */
    public ExplosionDefinition withImmuneFilter(ContextAwarePredicate immuneFilter) {
        return new ExplosionDefinition(radius, createFire, damageEntities, interaction, Optional.of(immuneFilter), smallParticles, largeParticles, sound);
    }

    /**
     * Returns a copy of this definition with custom small blast particles.
     *
     * @param smallParticles The small particle options.
     * @return A new {@link ExplosionDefinition} instance.
     */
    public ExplosionDefinition withSmallParticles(ParticleOptions smallParticles) {
        return new ExplosionDefinition(radius, createFire, damageEntities, interaction, immuneFilter, Optional.of(smallParticles), largeParticles, sound);
    }

    /**
     * Returns a copy of this definition with custom large blast emitter particles.
     *
     * @param largeParticles The large particle options.
     * @return A new {@link ExplosionDefinition} instance.
     */
    public ExplosionDefinition withLargeParticles(ParticleOptions largeParticles) {
        return new ExplosionDefinition(radius, createFire, damageEntities, interaction, immuneFilter, smallParticles, Optional.of(largeParticles), sound);
    }

    /**
     * Returns a copy of this definition with a custom detonation sound event.
     *
     * @param sound The sound event holder.
     * @return A new {@link ExplosionDefinition} instance.
     */
    public ExplosionDefinition withSound(Holder<SoundEvent> sound) {
        return new ExplosionDefinition(radius, createFire, damageEntities, interaction, immuneFilter, smallParticles, largeParticles, Optional.of(sound));
    }
    // endregion
}