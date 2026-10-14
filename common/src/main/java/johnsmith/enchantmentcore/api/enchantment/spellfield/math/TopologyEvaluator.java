package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.Set;

import johnsmith.enchantmentcore.api.registry.LevelBasedKeyProvider;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;

/**
 * Evaluates whether a discrete block or particle effect should trigger at a given coordinate based
 * on its local topological multiplier scalar.
 * <p>
 * If a deterministic {@link #threshold()} is present, the evaluator succeeds whenever
 * $\text{scalar} \ge \text{threshold}$. Otherwise, it operates probabilistically, succeeding with
 * probability equal to $\text{scalar}$.
 *
 * @param threshold An optional {@link LevelBasedValue} minimum scalar cutoff for deterministic activation.
 */
public record TopologyEvaluator(
        Optional<LevelBasedValue> threshold
) {
    /** JSON field key for the deterministic scalar cutoff threshold. */
    public static final String THRESHOLD = "threshold";

    /**
     * Supplies the JSON property keys holding {@link LevelBasedValue} definitions for data transformation.
     */
    public static final LevelBasedKeyProvider KEY_PROVIDER = () -> Set.of(THRESHOLD);

    /**
     * Codec for serializing and deserializing {@link TopologyEvaluator} instances from JSON.
     */
    public static final Codec<TopologyEvaluator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LevelBasedValue.CODEC.optionalFieldOf(THRESHOLD).forGetter(TopologyEvaluator::threshold)
    ).apply(instance, TopologyEvaluator::new));

    /**
     * Default probabilistic evaluator instance with no fixed threshold.
     */
    public static final TopologyEvaluator PROBABILISTIC = new TopologyEvaluator(Optional.empty());

    /**
     * Tests whether an effect should activate for the given topological {@code scalar}.
     *
     * @param scalar           The local topological multiplier in $[0.0, 1.0]$.
     * @param enchantmentLevel The active level of the enchantment.
     * @param random           The random source used when evaluating probabilistically.
     * @return {@code true} if the threshold or random roll succeeds, {@code false} otherwise.
     */
    public boolean evaluate(float scalar, int enchantmentLevel, RandomSource random) {
        if (this.threshold.isPresent()) {
            return scalar >= this.threshold.get().calculate(enchantmentLevel);
        }
        return scalar >= 1.0F || random.nextFloat() <= scalar;
    }

    /**
     * Returns the evaluated deterministic cutoff threshold at the given enchantment level,
     * or {@code 0.0F} if this evaluator is probabilistic.
     * <p>
     * Used by {@link johnsmith.enchantmentcore.api.enchantment.spellfield.effect.SpellFieldBlockEffect#getPriority(int)}
     * so higher-threshold inner-core block effects execute before outer-ring effects.
     *
     * @param enchantmentLevel The active level of the enchantment.
     * @return The threshold scalar, or {@code 0.0F}.
     */
    public float getThreshold(int enchantmentLevel) {
        return this.threshold.map(t -> t.calculate(enchantmentLevel)).orElse(0.0F);
    }
}