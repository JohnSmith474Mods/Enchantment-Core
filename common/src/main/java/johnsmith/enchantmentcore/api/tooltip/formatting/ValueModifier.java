package johnsmith.enchantmentcore.api.tooltip.formatting;

/**
 * Defines a functional contract for translating dynamic floating-point values into formatted string representations.
 * Executes during the recursive evaluation phase of the dynamic description parser.
 */
@FunctionalInterface
public interface ValueModifier {
    /**
     * Applies the formatting transformation to the specified value.
     *
     * @param arg   The optional string argument parsed from the modifier tag. Null if no argument is provided.
     * @param value The floating-point value extracted from the tooltip context.
     * @return The formatted string representation of the value.
     */
    String apply(String arg, float value);
}