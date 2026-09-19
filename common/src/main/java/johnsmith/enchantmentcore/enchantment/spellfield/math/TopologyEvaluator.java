package johnsmith.enchantmentcore.enchantment.spellfield.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public record TopologyEvaluator(
        Optional<LevelBasedValue> threshold
) {
    public static final String THRESHOLD = "threshold";

    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(THRESHOLD);

    public static final Codec<TopologyEvaluator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.optionalFieldOf(THRESHOLD).forGetter(TopologyEvaluator::threshold)
    ).apply(instance, TopologyEvaluator::new));

    public static final TopologyEvaluator PROBABILISTIC = new TopologyEvaluator(Optional.empty());

    public boolean evaluate(float scalar, int enchantmentLevel, RandomSource random) {
        if (this.threshold.isPresent()) {
            return scalar >= this.threshold.get().calculate(enchantmentLevel);
        }
        return scalar >= 1.0F || random.nextFloat() <= scalar;
    }

    public float getThreshold(int enchantmentLevel) {
        return this.threshold.map(t -> t.calculate(enchantmentLevel)).orElse(0.0F);
    }
}