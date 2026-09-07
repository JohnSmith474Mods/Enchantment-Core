package johnsmith.enchantmentcore.enchantment.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.Set;

public record ClimbingEffect(
        LevelBasedValue speed,
        boolean holdOnCrouch,
        Optional<HolderSet<Block>> allowedBlocks
) {
    public static final String SPEED = "speed";
    public static final String HOLD_ON_CROUCH = "hold_on_crouch";
    public static final String ALLOWED_BLOCKS = "allowed_blocks";

    public static final String KEY = "climbing";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(SPEED);

    public static final Codec<ClimbingEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.optionalFieldOf(SPEED, LevelBasedValue.constant(1.0F)).forGetter(ClimbingEffect::speed),
            Codec.BOOL.optionalFieldOf(HOLD_ON_CROUCH, false).forGetter(ClimbingEffect::holdOnCrouch),
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf(ALLOWED_BLOCKS).forGetter(ClimbingEffect::allowedBlocks)
    ).apply(instance, ClimbingEffect::new));
}