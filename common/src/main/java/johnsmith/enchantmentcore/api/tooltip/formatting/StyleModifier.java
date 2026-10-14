package johnsmith.enchantmentcore.api.tooltip.formatting;

import net.minecraft.network.chat.Style;

/**
 * Defines a functional contract for mutating text styles during tooltip rendering.
 * Executes during the component generation phase of the dynamic description parser.
 */
@FunctionalInterface
public interface StyleModifier {
    /**
     * Applies the style modification to the current text style.
     *
     * @param currentStyle The active text style in the parsing hierarchy.
     * @param arg          The optional string argument parsed from the modifier tag. Null if no argument is provided.
     * @return The mutated text style.
     */
    Style apply(Style currentStyle, String arg);
}