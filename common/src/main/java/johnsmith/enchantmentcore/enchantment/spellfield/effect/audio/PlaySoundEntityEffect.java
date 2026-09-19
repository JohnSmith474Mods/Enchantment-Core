package johnsmith.enchantmentcore.enchantment.spellfield.effect.audio;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldEntityEffect;
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

public record PlaySoundEntityEffect(
        Holder<SoundEvent> sound,
        SoundSource source,
        LevelBasedValue volume,
        LevelBasedValue pitch
) implements SpellFieldEntityEffect {

    public static final String SOUND = "sound";
    public static final String SOURCE = "source";
    public static final String VOLUME = "volume";
    public static final String PITCH = "pitch";

    public static final String KEY = "play_sound_entity";

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

    public static final MapCodec<PlaySoundEntityEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.SOUND_EVENT.holderByNameCodec().fieldOf(SOUND).forGetter(PlaySoundEntityEffect::sound),
            SOURCE_CODEC.optionalFieldOf(SOURCE, SoundSource.PLAYERS).forGetter(PlaySoundEntityEffect::source),
            LevelBasedValue.CODEC.optionalFieldOf(VOLUME, LevelBasedValue.constant(1.0F)).forGetter(PlaySoundEntityEffect::volume),
            LevelBasedValue.CODEC.optionalFieldOf(PITCH, LevelBasedValue.constant(1.0F)).forGetter(PlaySoundEntityEffect::pitch)
    ).apply(instance, PlaySoundEntityEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, Entity victim, float scalar) {
        float v = this.volume.calculate(enchantmentLevel) * scalar;
        if (v <= 0.01F) return;
        float p = this.pitch.calculate(enchantmentLevel);

        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), this.sound.value(), this.source, v, p);
    }

    @Override
    public MapCodec<? extends SpellFieldEntityEffect> codec() { return CODEC; }
}