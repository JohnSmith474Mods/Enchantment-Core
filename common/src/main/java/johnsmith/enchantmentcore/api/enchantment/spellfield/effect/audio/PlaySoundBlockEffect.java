package johnsmith.enchantmentcore.api.enchantment.spellfield.effect.audio;

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

/**
 * A {@link SpellFieldBlockEffect} that broadcasts a configured {@link SoundEvent} centered at each
 * evaluated block coordinate within a spell field volume, scaling the output volume by the block's
 * local topological multiplier.
 *
 * @param sound  The registry {@link Holder} of the {@link SoundEvent} to play.
 * @param source The {@link SoundSource} category channel through which the sound is played.
 * @param volume The {@link LevelBasedValue} base volume multiplier (further attenuated by the block's topological scalar).
 * @param pitch  The {@link LevelBasedValue} pitch multiplier applied to the sound event.
 */
public record PlaySoundBlockEffect(
        Holder<SoundEvent> sound,
        SoundSource source,
        LevelBasedValue volume,
        LevelBasedValue pitch
) implements SpellFieldBlockEffect {
    /** JSON field key for the sound event identifier. */
    public static final String SOUND = "sound";

    /** JSON field key for the sound source category. */
    public static final String SOURCE = "source";

    /** JSON field key for the base sound volume. */
    public static final String VOLUME = "volume";

    /** JSON field key for the sound pitch multiplier. */
    public static final String PITCH = "pitch";

    /** Registry path identifier for this spell field block effect. */
    public static final String KEY = "play_sound_block";

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
     * MapCodec for serializing and deserializing {@link PlaySoundBlockEffect} instances from JSON.
     */
    public static final MapCodec<PlaySoundBlockEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.SOUND_EVENT.holderByNameCodec().fieldOf(SOUND).forGetter(PlaySoundBlockEffect::sound),
            SOURCE_CODEC.optionalFieldOf(SOURCE, SoundSource.PLAYERS).forGetter(PlaySoundBlockEffect::source),
            LevelBasedValue.CODEC.optionalFieldOf(VOLUME, LevelBasedValue.constant(1.0F)).forGetter(PlaySoundBlockEffect::volume),
            LevelBasedValue.CODEC.optionalFieldOf(PITCH, LevelBasedValue.constant(1.0F)).forGetter(PlaySoundBlockEffect::pitch)
    ).apply(instance, PlaySoundBlockEffect::new));

    /**
     * Broadcasts the configured sound event at the center of {@code pos} if the effective volume
     * ($\text{volume} \cdot \text{scalar}$) exceeds {@code 0.01F}.
     *
     * @param level            The executing server level.
     * @param enchantmentLevel The active level of the spell field enchantment.
     * @param context          The item usage context triggering the spell field.
     * @param spatialReference The anchor entity or focal point of the spell field.
     * @param epicenter        The exact spatial origin vector of the spell field.
     * @param pos              The target block coordinate.
     * @param scalar           The active topological multiplier at {@code pos}.
     * @param modifiedBlocks   A mutable set tracking block coordinates modified during this evaluation cycle.
     */
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