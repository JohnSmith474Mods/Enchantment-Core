package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.audio;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVolumeEffect;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SpellFieldVolumeEffect} that broadcasts a single {@link SoundEvent} at the primary
 * {@code epicenter} of the spell field once per evaluation cycle.
 *
 * @param sound  The registry {@link Holder} of the {@link SoundEvent} to play.
 * @param source The {@link SoundSource} category channel through which the sound is played.
 * @param volume The {@link LevelBasedValue} governing the sound volume multiplier.
 * @param pitch  The {@link LevelBasedValue} governing the sound pitch multiplier.
 */
public record PlaySoundVolumeEffect(
        Holder<SoundEvent> sound,
        SoundSource source,
        LevelBasedValue volume,
        LevelBasedValue pitch
) implements SpellFieldVolumeEffect {
    /** JSON field key for the sound event identifier. */
    public static final String SOUND = "sound";

    /** JSON field key for the sound source category. */
    public static final String SOURCE = "source";

    /** JSON field key for the sound volume. */
    public static final String VOLUME = "volume";

    /** JSON field key for the sound pitch multiplier. */
    public static final String PITCH = "pitch";

    /** Registry path identifier for this spell field volume effect. */
    public static final String KEY = "play_sound_volume";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(VOLUME, PITCH);

    /**
     * Case-insensitive string codec for resolving {@link SoundSource} constants by name,
     * defaulting to {@link SoundSource#PLAYERS} if unrecognized.
     */
    public static final Codec<SoundSource> SOURCE_CODEC = Codec.STRING.xmap(
            string -> {
                for (SoundSource s : SoundSource.values()) {
                    if (s.getName().equalsIgnoreCase(string)) return s;
                }
                return SoundSource.PLAYERS;
            },
            SoundSource::getName
    );

    /**
     * MapCodec for serializing and deserializing {@link PlaySoundVolumeEffect} instances from JSON.
     */
    public static final MapCodec<PlaySoundVolumeEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.SOUND_EVENT.holderByNameCodec().fieldOf(SOUND).forGetter(PlaySoundVolumeEffect::sound),
            SOURCE_CODEC.optionalFieldOf(SOURCE, SoundSource.PLAYERS).forGetter(PlaySoundVolumeEffect::source),
            LevelBasedValue.CODEC.optionalFieldOf(VOLUME, LevelBasedValue.constant(1.0F)).forGetter(PlaySoundVolumeEffect::volume),
            LevelBasedValue.CODEC.optionalFieldOf(PITCH, LevelBasedValue.constant(1.0F)).forGetter(PlaySoundVolumeEffect::pitch)
    ).apply(instance, PlaySoundVolumeEffect::new));

    /**
     * Broadcasts the configured sound event at the spell field {@code epicenter} if the calculated
     * volume exceeds {@code 0.01F}.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param target           The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param volumes          The list of evaluated world-space {@link GlobalVolume}s defining the spell field geometry.
     */
    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity target, Vec3 epicenter, List<GlobalVolume> volumes) {
        float v = this.volume.calculate(enchantmentLevel);
        if (v <= 0.01F) return;

        float p = this.pitch.calculate(enchantmentLevel);
        level.playSound(null, epicenter.x, epicenter.y, epicenter.z, this.sound.value(), this.source, v, p);
    }

    @Override
    public MapCodec<? extends SpellFieldVolumeEffect> codec() { return CODEC; }
}