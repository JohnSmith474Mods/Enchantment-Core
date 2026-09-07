package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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
    public static final String RADIUS = "radius";
    public static final String CREATE_FIRE = "create_fire";
    public static final String DAMAGE_ENTITIES = "damage_entities";
    public static final String EXPLOSION_INTERACTION = "interaction";
    public static final String IMMUNE_FILTER = "immune_filter";
    public static final String SMALL_PARTICLES = "small_particles";
    public static final String LARGE_PARTICLES = "large_particles";
    public static final String SOUND = "sound";
    
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(RADIUS);

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

            level.explode(source, damageSource, damageCalculator, pos.x(), pos.y(), pos.z(), calculatedRadius, this.createFire, vanillaInteraction, true, small, large, snd);
        } else {
            level.explode(source, damageSource, damageCalculator, pos.x(), pos.y(), pos.z(), calculatedRadius, this.createFire, vanillaInteraction);
        }
    }

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

    public static Level.ExplosionInteraction getVanillaInteraction(ExplosionInteraction interaction) {
        return switch (interaction) {
            case NONE -> Level.ExplosionInteraction.NONE;
            case BLOCK -> Level.ExplosionInteraction.BLOCK;
            case MOB -> Level.ExplosionInteraction.MOB;
            case TNT -> Level.ExplosionInteraction.TNT;
            case TRIGGER -> Level.ExplosionInteraction.TRIGGER;
        };
    }

    public enum ExplosionInteraction implements StringRepresentable {
        NONE("none"),
        BLOCK("block"),
        MOB("mob"),
        TNT("tnt"),
        TRIGGER("trigger");

        public static final Codec<ExplosionInteraction> CODEC = StringRepresentable.fromEnum(ExplosionInteraction::values);
        private final String id;

        ExplosionInteraction(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }
    }
}