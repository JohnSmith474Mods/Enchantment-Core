package johnsmith.enchantmentcore.enchantment.spellfield.effect.audio;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.math.GlobalVolume;
import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldVolumeEffect;

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

public record PlaySoundVolumeEffect(
        Holder<SoundEvent> sound,
        SoundSource source,
        LevelBasedValue volume,
        LevelBasedValue pitch
) implements SpellFieldVolumeEffect {

    public static final String SOUND = "sound";
    public static final String SOURCE = "source";
    public static final String VOLUME = "volume";
    public static final String PITCH = "pitch";

    public static final String KEY = "play_sound_volume";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(VOLUME, PITCH);

    public static final Codec<SoundSource> SOURCE_CODEC = Codec.STRING.xmap(
            string -> {
                for (SoundSource s : SoundSource.values()) {
                    if (s.getName().equalsIgnoreCase(string)) return s;
                }
                return SoundSource.PLAYERS;
            },
            SoundSource::getName
    );

    public static final MapCodec<PlaySoundVolumeEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.SOUND_EVENT.holderByNameCodec().fieldOf(SOUND).forGetter(PlaySoundVolumeEffect::sound),
            SOURCE_CODEC.optionalFieldOf(SOURCE, SoundSource.PLAYERS).forGetter(PlaySoundVolumeEffect::source),
            LevelBasedValue.CODEC.optionalFieldOf(VOLUME, LevelBasedValue.constant(1.0F)).forGetter(PlaySoundVolumeEffect::volume),
            LevelBasedValue.CODEC.optionalFieldOf(PITCH, LevelBasedValue.constant(1.0F)).forGetter(PlaySoundVolumeEffect::pitch)
    ).apply(instance, PlaySoundVolumeEffect::new));

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