package johnsmith.enchantmentcore.api.tooltip.formatting;

/**
 * Functional contract for transforming evaluated floating-point numbers into formatted strings
 * during dynamic enchantment description parsing.
 * <p>
 * Registered in {@link johnsmith.enchantmentcore.api.tooltip.DynamicDescriptionFormatter} and invoked
 * inside-out when evaluating tags of the form {@code <modifier:arg:value>} or {@code <modifier:value>}.
 */
@FunctionalInterface
public interface ValueModifier {
    /**
     * Applies a mathematical or formatting transformation to the specified numeric value.
     *
     * @param arg   The optional string argument parsed from the modifier tag (e.g., divisor or decimal precision), or {@code null} if omitted.
     * @param value The floating-point value being transformed.
     * @return The formatted string representation of the transformed value.
     */
    String apply(String arg, float value);
}