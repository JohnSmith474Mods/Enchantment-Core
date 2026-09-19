package johnsmith.enchantmentcore.enchantment.spellfield.effect.audio;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Set;

import johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect;
import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record PlaySoundBlockEffect(
        Holder<SoundEvent> sound,
        SoundSource source,
        LevelBasedValue volume,
        LevelBasedValue pitch
) implements SpellFieldBlockEffect {

    public static final String SOUND = "sound";
    public static final String SOURCE = "source";
    public static final String VOLUME = "volume";
    public static final String PITCH = "pitch";

    public static final String KEY = "play_sound_block";

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

    public static final MapCodec<PlaySoundBlockEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.SOUND_EVENT.holderByNameCodec().fieldOf(SOUND).forGetter(PlaySoundBlockEffect::sound),
            SOURCE_CODEC.optionalFieldOf(SOURCE, SoundSource.PLAYERS).forGetter(PlaySoundBlockEffect::source),
            LevelBasedValue.CODEC.optionalFieldOf(VOLUME, LevelBasedValue.constant(1.0F)).forGetter(PlaySoundBlockEffect::volume),
            LevelBasedValue.CODEC.optionalFieldOf(PITCH, LevelBasedValue.constant(1.0F)).forGetter(PlaySoundBlockEffect::pitch)
    ).apply(instance, PlaySoundBlockEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse context, Entity spatialReference, Vec3 epicenter, BlockPos pos, float scalar, Set<BlockPos> modifiedBlocks) {
        float v = this.volume.calculate(enchantmentLevel) * scalar;
        if (v <= 0.01F) return;
        float p = this.pitch.calculate(enchantmentLevel);

        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, this.sound.value(), this.source, v, p);
    }

    @Override
    public MapCodec<? extends SpellFieldBlockEffect> codec() { return CODEC; }
}